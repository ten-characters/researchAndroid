package com.ten_characters.researchAndroid.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by austin on 13/05/15.
 * Creates a simple and regulated way to store and reach the data pulled from our servers! Howdee do!
 */
public class PalletProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    // Set codes to be mapped to each type of uri.
    // Uri Matchers make it less bulky to reference and compare to uris

    private static final int ALL_SHIPMENTS = 100;
    private static final int SHIPMENT_WITH_OBJ_ID = 101;
    private static final int SHIPMENT_WITH_DB_ID = 102;
    private static final int SHIPMENTS_WITH_RANGE = 103;

    private static final int FAVORITE_SHIPMENT = 200;
    private static final int FAVORITE_SHIPMENT_WITH_OBJ_ID = 201;

    private PalletDbHelper mOpenHelper;
    private static final SQLiteQueryBuilder sShipmentQueryBuilder, sFavoriteQueryBuilder;

    // Initialize the queryBuilder
    static {
        sShipmentQueryBuilder = new SQLiteQueryBuilder();
        sShipmentQueryBuilder.setTables(
                PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME);

        sFavoriteQueryBuilder = new SQLiteQueryBuilder();
        sFavoriteQueryBuilder.setTables(
                PalletDbContract.FavoriteShipmentEntry.TABLE_NAME);

    }

    /** Selections for the Tables */
    private static final String sUnacceptedByIDSelection =
            "." + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " = ? ";
    private static final String sUnacceptedByRangeSelection =
            "." + PalletDbContract.UnacceptedShipmentEntry.COLUMN_RANGE_MILES + " <= ? ";


    /** Functions to return the Data! This is what we're here for!*/
    private Cursor getUnaccepted(@Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return sShipmentQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getUnacceptedByRange(Uri uri, String[] projection, String sortOrder, int rangeInMiles) {
        // TODO: store range in preferences or calculate it by the scale of the map
        String[] selectionArgs = {Integer.toString(rangeInMiles)};
        String selection = sUnacceptedByRangeSelection;

        return sShipmentQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    // Just search for one object id!
    private Cursor getUnacceptedByObjId(String[] projection, String objId) {
        String selection = PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME +
                "." + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " = ?";
        return sShipmentQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                new String[]{objId},
                null,
                null,
                null
        );
    }

    private Cursor getFavoriteShipments(String[] shipmentProjection) {
        // First query all toggleFavorite ids
        // A hackaround way to do it but well fuck you sqlite

        // Query all the favorites
        final String [] objIdProjection = {
                PalletDbContract.FavoriteShipmentEntry.TABLE_NAME + "." + PalletDbContract.FavoriteShipmentEntry._ID,
                PalletDbContract.FavoriteShipmentEntry.COLUMN_SHIPMENT_OBJ_ID
        };

        Cursor favoriteCursor =  sFavoriteQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                objIdProjection,
                null,
                null,
                null,
                null,
                null);


        Cursor[] cursors = new Cursor[favoriteCursor.getCount()];
        // Load all the favorites
        while (favoriteCursor.moveToNext()) {
            cursors[favoriteCursor.getPosition()] = getUnacceptedByObjId(shipmentProjection, favoriteCursor.getString(1));
        }

        if (favoriteCursor.getCount() == 0) {
            return favoriteCursor;
        }

        // Having a problem where the favorite cursor loader doesn't update when all these cursors merged
        //favoriteCursor.notifyAll();
        return new MergeCursor(cursors);
    }

    public static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PalletDbContract.CONTENT_AUTHORITY;

        // Map each type of uri that can be returned
        // Shipments
        matcher.addURI(authority, PalletDbContract.PATH_UNACCEPTED, ALL_SHIPMENTS);
        matcher.addURI(authority, PalletDbContract.PATH_UNACCEPTED + "/*", SHIPMENT_WITH_OBJ_ID);
        // Favorite Shipments
        matcher.addURI(authority, PalletDbContract.PATH_FAVORITE, FAVORITE_SHIPMENT);
        matcher.addURI(authority, PalletDbContract.PATH_FAVORITE + "/*", FAVORITE_SHIPMENT_WITH_OBJ_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new PalletDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // given a URI, determines which kind of request it is! Then will query the right database
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "unaccepted"
            case ALL_SHIPMENTS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // "unaccepted/*
            case SHIPMENT_WITH_OBJ_ID:
                retCursor = getUnacceptedByObjId(projection, PalletDbContract.UnacceptedShipmentEntry.getShipmentObjIdFromUri(uri));
                break;
            case FAVORITE_SHIPMENT:
                retCursor = getFavoriteShipments(projection);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {

        // Determine the kind of Uri
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case ALL_SHIPMENTS:
                return PalletDbContract.UnacceptedShipmentEntry.CONTENT_TYPE;
            case SHIPMENTS_WITH_RANGE:
                return PalletDbContract.UnacceptedShipmentEntry.CONTENT_TYPE;
            case SHIPMENT_WITH_OBJ_ID:
                return PalletDbContract.UnacceptedShipmentEntry.CONTENT_ITEM_TYPE;
            case FAVORITE_SHIPMENT:
                return PalletDbContract.FavoriteShipmentEntry.CONTENT_TYPE;
            case FAVORITE_SHIPMENT_WITH_OBJ_ID:
                return PalletDbContract.FavoriteShipmentEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case ALL_SHIPMENTS: {
                long _id = db.insert(PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME,
                        null,
                        values);
                if (_id > 0)
                    returnUri = PalletDbContract.UnacceptedShipmentEntry.buildShipmentWithDbIdUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case FAVORITE_SHIPMENT: {
                long _id = db.insert(PalletDbContract.FavoriteShipmentEntry.TABLE_NAME,
                        null,
                        values);
                if (_id > 0) {
                    returnUri = PalletDbContract.FavoriteShipmentEntry.buildFavoriteWithDbIdUri(_id);
                    getContext().getContentResolver().notifyChange(PalletDbContract.FavoriteShipmentEntry.buildFavoriteShipmentsUri(), null);
                }
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Should only return the returnUri if successful
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        // Todo: find a way to only delete those not there, or just query server for a smaller portion

        switch (match) {
            case ALL_SHIPMENTS:
            case FAVORITE_SHIPMENT:
                String tableName;
                if (match == ALL_SHIPMENTS) {
                    tableName = PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME;
                    db.execSQL("DELETE FROM " + PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME);
                } else {
                    tableName = PalletDbContract.FavoriteShipmentEntry.TABLE_NAME;
                    db.execSQL("DELETE FROM " + PalletDbContract.FavoriteShipmentEntry.TABLE_NAME);
                }

                db.beginTransaction();
                int numInserted = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(tableName,
                                null,
                                value);
                        if (_id != -1) {
                            // -1 is only returned by the insert function if the insert fails
                            numInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    // Must close the transaction NO MATTER WHAT
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return numInserted;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        switch (match) {
            case ALL_SHIPMENTS:
                if (selection == null || selectionArgs == null) {
                    // Delete all from the table!
                    rowsDeleted = getUnaccepted(null, null, null, null).getCount();
                    db.execSQL("DELETE FROM " + PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME);
                }
                else
                    rowsDeleted = db.delete(
                            PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FAVORITE_SHIPMENT:
                if (selection == null || selectionArgs == null) {
                    // Delete all from the table!
                    Cursor favCursor = getFavoriteShipments(null);
                    if (favCursor != null)
                        rowsDeleted = favCursor.getCount();
                    else
                        rowsDeleted = 0 ;
                    db.execSQL("DELETE FROM " + PalletDbContract.FavoriteShipmentEntry.TABLE_NAME);
                }
                else {
                    rowsDeleted = db.delete(
                            PalletDbContract.FavoriteShipmentEntry.TABLE_NAME, selection, selectionArgs);
                }
                break;
            case FAVORITE_SHIPMENT_WITH_OBJ_ID:
                selection = PalletDbContract.FavoriteShipmentEntry.TABLE_NAME +
                    "." + PalletDbContract.FavoriteShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " = ?";
                String[] args = {PalletDbContract.FavoriteShipmentEntry.getShipmentObjIdFromUri(uri)};
                rowsDeleted = db.delete(
                        PalletDbContract.FavoriteShipmentEntry.TABLE_NAME,
                        selection,
                        args);
                if (rowsDeleted != 0)
                    getContext().getContentResolver().notifyChange(PalletDbContract.FavoriteShipmentEntry.buildFavoriteShipmentsUri(), null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Passing a "null" selection deletes all the rows in the database
        // Only notify of a change if there actually was a change!
        if(selection == null || rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case ALL_SHIPMENTS:
                rowsUpdated = db.update(
                        PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }
}
