package com.ten_characters.researchAndroid.server;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by austin on 24/06/15.
 */
public class ServerUtility {
    // PRODUCTION
    public static final String BASE_API_URL = "https://api.serveraddress.com";
    public static final String BASE_WEB_URL = "https://serveraddress.com/";

    // TESTING
//    public static final String BASE_API_URL = "https://test.serveraddress.com/api";
//    public static final String BASE_WEB_URL = "https://test.serveraddress.com/";

    // LOCAL ROUTER
//    public static final String BASE_API_URL = "http://192.168.1.6/api";
//    public static final String BASE_WEB_URL = "http://192.168.1.6";

    public static final String API_VERSION_1 = "v1.0";
    public static final String API_VERSION_1_1 = "v1.1";

    public static final String PALLET_EMAIL = "info@serveraddress.com";

    // EXTENSIONS
    public static final String ANDROID_EXT = "android";
    public static final String LOGIN_EXT = "login";
    public static final String ADD_PAYMENT_EXT = "add_payment";
    public static final String APPLY_EXT = "apply";
    public static final String REGISTER_EXT = "register";
    public static final String USER_EXT = "user";
    public static final String DRIVER_EXT = "driver";
    public static final String DRIVERS_EXT = "drivers";
    public static final String REFERRAL_CODE_EXT = "ref";
    public static final String TOG_ACTIVE_EXT = "toggle_active";
    public static final String SHIPMENTS_EXT = "shipments";
    public static final String UNACCEPTED_EXT = "unaccepted";
    public static final String RESPOND_EXT = "respond";
    public static final String GET_MINE_EXT = "get_mine";

    // KEYS
    public static final String RESULT_KEY = "result";

    public static final String ID_KEY = "_id";
    public static final String OBJ_ID_KEY = "$oid";
    public static final String CUSTOMER_ID_KEY = "customer_id";
    public static final String CLIENT_TOKEN_KEY = "client_token";
    public static final String ERROR_CODE_KEY = "code";
    public static final String USER_KEY = "user";
    public static final String EMAIL_KEY = "email";
    public static final String PASSWORD_KEY = "password";
    public static final String AUTH_TOKEN_KEY = "token";
    public static final String NEW_TOKEN_KEY = "new_token";
    public static final String DRIVER_ID_KEY = "driver_id";
    public static final String SHIPMENT_ID_KEY = "shipment_id";
    public static final String SHIPMENT_KEY = "shipment";
    public static final String MAX_RETURNED_KEY = "max_returned";
    public static final String ONLY_CURRENT_KEY = "only_current";
    public static final String RANGE_KEY = "rng";

    // FOR SHIPMENTS
    public static final String PRICE_KEY = "trucker_price";
    public static final String COMMODITY_KEY = "commodity";
    public static final String REFERENCE_NUMS_KEY = "reference_numbers";
    public static final String PRIMARY_REF_KEY = "Primary";
    public static final String WEIGHT_KEY = "weight";
    public static final String IS_FULL_TRUCKLOAD_KEY = "is_full_truckload";
    public static final String NUM_PALLETS_KEY = "num_pallets";
    public static final String NUM_PIECES_KEY = "num_pieces_per_pallet";

    public static final String PICKUP_NAME_KEY = "pickup_name";
    public static final String PICKUP_LOC_KEY = "pickup_location";
    public static final String PICKUP_ADDR_KEY = "pickup_address";
    public static final String PICKUP_TIME_KEY = "pickup_time";
    public static final String PICKUP_TIME_END_KEY = "pickup_time_end";
    public static final String PICKUP_RATING_KEY = "pickup_rating";

    public static final String DROPOFF_NAME_KEY = "dropoff_name";
    public static final String DROPOFF_LOC_KEY = "dropoff_location";
    public static final String DROPOFF_ADDR_KEY = "dropoff_address";
    public static final String DROPOFF_TIME_KEY = "dropoff_time";
    public static final String DROPOFF_TIME_END_KEY = "dropoff_time_end";
    public static final String DROPOFF_RATING_KEY = "dropoff_rating";

    public static final String NEEDS_LIFT_KEY = "needs_liftgate";
    public static final String NEEDS_LUMP_KEY = "needs_jack";
    public static final String NEEDS_JACK_KEY = "needs_lumper";
    public static final String IS_IN_TRANSIT_KEY = "is_in_transit";
    public static final String IS_AVAILABLE_KEY = "is_available";
    public static final String IS_AVAILABLE_IN_SECONDS_KEY = "to_available_seconds";
    public static final String IS_FINISHED_KEY = "is_finished";
    public static final String TIME_FINISHED_KEY = "time_finished";

    public static final String PICKUP_CONTACT_KEY = "start_contact";
    public static final String DROPOFF_CONTACT_KEY = "end_contact";
    public static final String NAME_KEY = "name";

    // FOR USERS
    // KEYS TO GET FROM SERVED DATA
    public static final String FIRST_NAME_KEY = "first_name";
    public static final String LAST_NAME_KEY = "last_name";
    public static final String COMPANY_KEY = "company";
    public static final String PHONE_KEY = "phone";
    public static final String NOTIF_KEY = "notification_key";
    public static final String REFERRAL_CODE_KEY = "referral_code";
    public static final String USER_TYPE_KEY = "user_type";
    public static final String WAREHOUSES_KEY = "warehouses";
    public static final String SHIPMENTS_KEY = "shipments";
    public static final String FINISHED_SHIPMENTS_KEY = "finished_shipments";
    public static final String RATING_KEY = "rating";

    public static final String CURRENT_ADDR_KEY = "current_address";
    public static final String BILLING_INFO_KEY = "billing_info";
        public static final String ADDRESS_KEY = "address";
        public static final String CITY_KEY = "city";
        public static final String STATE_KEY = "state";
        public static final String COUNTRY_KEY = "country";
        public static final String ZIP_KEY = "zip";

    public static final String DRIVER_INFO_KEY = "driver_info";
        public static final String IS_ACTIVE_KEY = "is_active";
        public static final String FAVORITES_KEY = "favorites";
        public static final String PAYMENT_CONFIRMED_KEY = "payment_confirmed";
        public static final String IFTA_KEY = "ifta_form_path";
        public static final String IRP_KEY = "irp_form_path";
        public static final String INSURANCE_KEY = "insurance_form_path";
        public static final String PROFILE_PATH_KEY = "profile_picture_path";
        public static final String LICENSE_KEY = "license_path";
        public static final String TRAILERS_KEY = "trailers";
        public static final String TRUCKS_KEY = "trucks";
            public static final String VIN_KEY = "vin";
            public static final String PLATE_KEY = "plate";
            public static final String YEAR_KEY = "year";
            public static final String MODEL_KEY = "model";
            public static final String MODEL_TYPE_KEY = "model_type";
            public static final String PHOTO_PATH_KEY = "photo_path";
            public static final String EQUIP_SIZE_KEY = "size";



    public static final String LOCATION_KEY = "location";
    public static final String FILE_PATH_KEY = "file_path";
    public static final String BOL_PATH_KEY = "bill_lading_path";
    public static final String PROFILE_IMAGE_TYPE = "profile_picture";

    public static final String BOL_TYPE = "bill_lading";

    public static String getGoogleDirectionsUriString(LatLng start, LatLng end) {
        String strStart = "origin=" + start.latitude + "," + start.longitude;
        String strEnd = "destination=" + end.latitude + "," + end.longitude;

        String sensor = "sensor=false";

        String durationInTraffic = "durationInTraffic=true";

        String params = strStart + "&" + strEnd + "&" + durationInTraffic + "&" + sensor;

        String outputFormat = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/"
                + outputFormat + "?" + params;

        return url;
    }
}
