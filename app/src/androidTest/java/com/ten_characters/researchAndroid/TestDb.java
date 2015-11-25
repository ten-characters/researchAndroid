package com.ten_characters.researchAndroid;

import android.test.AndroidTestCase;

/**
 * Created by austin on 13/05/15.
 */
public class TestDb extends AndroidTestCase{

    /*public class PalletDbContract {

        // Name for the entire content provider
        public static final String CONTENT_AUTHORITY = "com.truckpallet.pallet";

        // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
        // the content provider.
        public final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

        // PATHs to data: these are appended to the BASE URI for possible locations of data! Woo!
        public static final String PATH_UNACCEPTED = "unaccepted_shipments";
        public static final String PATH_FAVORITE = "favorite_shipments";

        *//** Defines table contents for the Local Drivers*//*
        public final class UnacceptedShipmentEntry implements BaseColumns {

            private final String LOG_TAG = UnacceptedShipmentEntry.class.getSimpleName();

            public final Uri CONTENT_URI =
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_UNACCEPTED).build();

            // Defines that we will receive a cursor from this uri
            public static final String CONTENT_TYPE =
                    "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_UNACCEPTED;
            public static final String CONTENT_ITEM_TYPE =
                    "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_UNACCEPTED;

            // Name of the Data Table
            public static final String TABLE_NAME = "unaccepted_shipments";

            // Query Paths!
            public static final String ID_PATH = "id";
            public static final String OBJ_ID_PATH = "obj_id";
            public static final String RANGE_PATH = "range";

            // COLUMNS of the SQLite local database
            // Stores the Range statically: doesn't calculate the range locally, only updated by server
            public static final String COLUMN_SHIPMENT_OBJ_ID = "obj_id";
            public static final String COLUMN_PRICE = "price";
            public static final String COLUMN_COMMODITY = "commodity";
            public static final String COLUMN_WEIGHT = "weight";
            public static final String COLUMN_IS_FULL_TRUCKLOAD = "is_full_truckload";
            public static final String COLUMN_IS_AVAILABLE = "is_available";
            public static final String COLUMN_NUM_PALLETS = "num_pallets";
            public static final String COLUMN_NUM_PIECES_PER_PALLET = "num_pieces_per_pallet";
            public static final String COLUMN_REF_NUMBERS_STRING = "ref_numbers";

            public static final String COLUMN_PICKUP_NAME = "pickup_name";
            public static final String COLUMN_PICKUP_RATING = "pickup_rating";
            public static final String COLUMN_PICKUP_LAT = "pickup_lat";
            public static final String COLUMN_PICKUP_LNG = "pickup_lng";
            public static final String COLUMN_PICKUP_TIME = "pickup_time";
            public static final String COLUMN_PICKUP_TIME_END = "pickup_time_end";
            public static final String COLUMN_PICKUP_CITY = "pickup_city";
            public static final String COLUMN_PICKUP_STATE = "pickup_state";
            public static final String COLUMN_PICKUP_ZIP = "pickup_zip";
            public static final String COLUMN_PICKUP_COUNTRY = "pickup_country";
            public static final String COLUMN_PICKUP_CONTACT_CSV = "pickup_contact";

            public static final String COLUMN_DROPOFF_NAME = "dropoff_name";
            public static final String COLUMN_DROPOFF_RATING = "dropoff_rating";
            public static final String COLUMN_DROPOFF_LAT = "dropoff_lat";
            public static final String COLUMN_DROPOFF_LNG = "dropoff_lng";
            public static final String COLUMN_DROPOFF_TIME = "dropoff_time";
            public static final String COLUMN_DROPOFF_TIME_END = "dropoff_time_end";
            public static final String COLUMN_DROPOFF_CITY = "dropoff_city";
            public static final String COLUMN_DROPOFF_STATE = "dropoff_state";
            public static final String COLUMN_DROPOFF_ZIP = "dropoff_zip";
            public static final String COLUMN_DROPOFF_COUNTRY = "dropoff_country";
            public static final String COLUMN_DROPOFF_CONTACT_CSV = "dropoff_contact";

            public static final String COLUMN_NEEDS_LIFT = "needs_lift";
            public static final String COLUMN_NEEDS_LUMPER = "needs_lumper";
            public static final String COLUMN_NEEDS_JACK = "needs_jack";

            public static final String COLUMN_RANGE_MILES = "rng_miles";

            // Methods for getting custom URIs with parameters
            public Uri buildUnacceptedShipmentsUri() {
                return CONTENT_URI;
            }


            public Uri buildShipmentWithDbIdUri(long id) {
                return CONTENT_URI.buildUpon().appendPath(ID_PATH)
                        .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, Long.toString(id)).build();
            }

            public Uri buildShipmentWithObjIdUri(String id) {
                return CONTENT_URI.buildUpon().appendPath(OBJ_ID_PATH)
                        .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, id).build();
            }

            public Uri buildShipmentsWithRangeUri(LatLng location, double range) {
                // Todo: write a range getter
                // Should be given a current location and then builds the range from there
                // The Range Column is just a placeholder for now
                return CONTENT_URI.buildUpon().appendPath(RANGE_PATH)
                        .appendQueryParameter(COLUMN_RANGE_MILES, Double.toString(range)).build();
            }
        }

        public class FavoriteShipmentEntry implements BaseColumns {

            private final String LOG_TAG = UnacceptedShipmentEntry.class.getSimpleName();

            public final Uri CONTENT_URI =
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE).build();

            // Defines that we will receive a cursor from this uri
            public static final String CONTENT_TYPE =
                    "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITE;
            public static final String CONTENT_ITEM_TYPE =
                    "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITE;

            // Name of the Data Table
            public static final String TABLE_NAME = "favorites";

            // Query paths!
            public static final String OBJ_ID_PATH = "obj_id";

            // Columns!
            public static final String COLUMN_SHIPMENT_OBJ_ID = "obj_id";

            public Uri buildFavoriteShipmentsUri() {
                return CONTENT_URI;
            }

            public Uri buildFavoriteWithObjIdUri(String id) {
                return CONTENT_URI.buildUpon().appendPath(OBJ_ID_PATH)
                        .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, id).build();
            }

        }
    }

    public class PalletDbHelper extends SQLiteOpenHelper {
        // Don't forget to update database version when schema changes
        private static final int DATABASE_VERSION = 6;
        public static final String DATABASE_NAME = "pallet.db";

        public PalletDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        final String

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String SQL_CREATE_SHIPMENTS_TABLE = "CREATE TABLE "
                    + "Unaccepted "
                    + " (" + PalletDbContract.UnacceptedShipmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " TEXT NOT NULL,"
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PRICE + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_COMMODITY + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_WEIGHT + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_IS_FULL_TRUCKLOAD + " NUMERIC NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_IS_AVAILABLE + " NUMERIC NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_NUM_PALLETS + " REAL NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_NUM_PIECES_PER_PALLET + " REAL NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_REF_NUMBERS_STRING + " TEXT NOT NULL, "

                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_NAME + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_RATING + " REAL NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LAT + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LNG + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_TIME + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_TIME_END + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_CITY + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_STATE + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_COUNTRY + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_ZIP + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_CONTACT_CSV + " TEXT NOT NULL, "

                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_NAME + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_RATING + " REAL NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_LAT + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_LNG + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME_END + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_CITY + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_STATE + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_COUNTRY + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_ZIP + " TEXT NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_CONTACT_CSV + " TEXT NOT NULL, "

                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_NEEDS_LIFT + " NUMERIC NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_NEEDS_LUMPER + " NUMERIC NOT NULL, "
                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_NEEDS_JACK + " NUMERIC NOT NULL, "

                    + PalletDbContract.UnacceptedShipmentEntry.COLUMN_RANGE_MILES + " REAL NOT NULL, "

                    // Can't be having the same shipment as two shipments, that'd be Ludacris! And Fraud!
                    + " UNIQUE (" + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + ") ON CONFLICT REPLACE"
                    + ");";

            final String SQL_CREATE_FAVORITES_TABLE = "CREATE TABLE "
                    + PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME
                    + " (" + PalletDbContract.FavoriteShipmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + PalletDbContract.FavoriteShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + " TEXT NOT NULL, "
                    +  " FOREIGN KEY (" + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + ") REFERENCES "
                    + PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME + " (" + PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID + ") )"
                    + ");";

            db.execSQL(SQL_CREATE_SHIPMENTS_TABLE);
            db.execSQL(SQL_CREATE_FAVORITES_TABLE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME);
            onCreate(db);
        }
    }

    SQLiteDatabase db = (new com.truckpallet.pallet.data.PalletDbHelper(mContext)).getReadableDatabase();*/
}
