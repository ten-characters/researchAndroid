package com.ten_characters.researchAndroid.data;

import android.net.Uri;
import android.provider.BaseColumns;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by austin on 13/05/15.
 *  Defines table and column names for the driver database
 *      The database will be used to read the current location of active drivers around
 */
public class PalletDbContract {

    // Name for the entire content provider
    public static final String CONTENT_AUTHORITY = "com.truckpallet.pallet";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // PATHs to data: these are appended to the BASE URI for possible locations of data! Woo!
    public static final String PATH_UNACCEPTED = "unaccepted_shipments";
    public static final String PATH_FAVORITE = "favorite_shipments";

    /** Defines table contents for the Local Drivers*/
    public static final class UnacceptedShipmentEntry implements BaseColumns {

        private static final String LOG_TAG = UnacceptedShipmentEntry.class.getSimpleName();

        public static final Uri CONTENT_URI =
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
//        public static final String COLUMN_IS_AVAILABLE = "is_available";
//        public static final String COLUMN_IS_AVAILABLE_IN_SECONDS = "is_available_in";
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
        public static Uri buildUnacceptedShipmentsUri() {
            return CONTENT_URI;
        }


        public static Uri buildShipmentWithDbIdUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(ID_PATH)
                    .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, Long.toString(id)).build();
        }

        public static Uri buildShipmentWithObjIdUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(OBJ_ID_PATH)
                    .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, id).build();
        }

        public static Uri buildShipmentsWithRangeUri(LatLng location, double range) {
            // Todo: write a range getter
            // Should be given a current location and then builds the range from there
            // The Range Column is just a placeholder for now
            return CONTENT_URI.buildUpon().appendPath(RANGE_PATH)
                    .appendQueryParameter(COLUMN_RANGE_MILES, Double.toString(range)).build();
        }

        public static String getShipmentObjIdFromUri(Uri uri) {
            return uri.getQueryParameter(OBJ_ID_PATH);
        }

    }

    public static final class FavoriteShipmentEntry implements BaseColumns {

        private static final String LOG_TAG = UnacceptedShipmentEntry.class.getSimpleName();

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE).build();

        // Defines that we will receive a cursor from this uri
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITE;

        // Name of the Data Table
        public static final String TABLE_NAME = "favorites";

        // Query paths!
        public static final String ID_PATH = "id";
        public static final String OBJ_ID_PATH = "obj_id";

        // Columns!
        public static final String COLUMN_SHIPMENT_OBJ_ID = "obj_id";

        public static Uri buildFavoriteShipmentsUri() {
            return CONTENT_URI;
        }

        public static Uri buildFavoriteWithDbIdUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(ID_PATH)
                    .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, Long.toString(id)).build();
        }

        public static Uri buildFavoriteWithObjIdUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(OBJ_ID_PATH)
                    .appendQueryParameter(COLUMN_SHIPMENT_OBJ_ID, id).build();
        }

        public static String getShipmentObjIdFromUri(Uri uri) {
            return uri.getQueryParameter(OBJ_ID_PATH);
        }

    }
}
