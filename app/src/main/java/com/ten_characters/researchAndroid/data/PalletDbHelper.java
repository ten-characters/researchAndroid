package com.ten_characters.researchAndroid.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ten_characters.researchAndroid.data.PalletDbContract.*;

/**
 * Created by austin on 13/05/15.
 */
public class PalletDbHelper extends SQLiteOpenHelper{
    // Don't forget to update database version when schema changes
    private static final int DATABASE_VERSION = 11;
    public static final String DATABASE_NAME = "pallet.db";

    public PalletDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_SHIPMENTS_TABLE = "CREATE TABLE "
                + UnacceptedShipmentEntry.TABLE_NAME
                    + " (" + UnacceptedShipmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " TEXT NOT NULL,"
                            + UnacceptedShipmentEntry.COLUMN_PRICE + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_COMMODITY + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_WEIGHT + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_IS_FULL_TRUCKLOAD + " NUMERIC NOT NULL, "
//                            + UnacceptedShipmentEntry.COLUMN_IS_AVAILABLE + " NUMERIC NOT NULL, "
//                            + UnacceptedShipmentEntry.COLUMN_IS_AVAILABLE_IN_SECONDS + " REAL NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_NUM_PALLETS + " REAL NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_NUM_PIECES_PER_PALLET + " REAL NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_REF_NUMBERS_STRING + " TEXT NOT NULL, "

                            + UnacceptedShipmentEntry.COLUMN_PICKUP_NAME + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_RATING + " REAL NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_LAT + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_LNG + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_TIME + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_TIME_END + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_CITY + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_STATE + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_COUNTRY + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_ZIP + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_PICKUP_CONTACT_CSV + " TEXT NOT NULL, "

                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_NAME + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_RATING + " REAL NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_LAT + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_LNG + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME_END + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_CITY + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_STATE + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_COUNTRY + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_ZIP + " TEXT NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_DROPOFF_CONTACT_CSV + " TEXT NOT NULL, "

                            + UnacceptedShipmentEntry.COLUMN_NEEDS_LIFT + " NUMERIC NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_NEEDS_LUMPER + " NUMERIC NOT NULL, "
                            + UnacceptedShipmentEntry.COLUMN_NEEDS_JACK + " NUMERIC NOT NULL, "

                            + UnacceptedShipmentEntry.COLUMN_RANGE_MILES + " REAL NOT NULL, "

                                // Can't be having the same shipment as two shipments, that'd be Ludacris! And Fraud!
                            + " UNIQUE (" + UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + ") ON CONFLICT REPLACE"
                    + ");";

        final String SQL_CREATE_FAVORITES_TABLE = "CREATE TABLE "
                + FavoriteShipmentEntry.TABLE_NAME
                + " (" + FavoriteShipmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + FavoriteShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " TEXT NOT NULL, "
                        + " UNIQUE (" + UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + ") ON CONFLICT REPLACE"
                        + ");";

        db.execSQL(SQL_CREATE_SHIPMENTS_TABLE);
        db.execSQL(SQL_CREATE_FAVORITES_TABLE);

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + UnacceptedShipmentEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + FavoriteShipmentEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + UnacceptedShipmentEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + FavoriteShipmentEntry.TABLE_NAME);
        onCreate(db);
    }
}
