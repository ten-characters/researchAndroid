package com.ten_characters.researchAndroid;

/**
 * Created by austin on 24/06/15.
 */
public class DocumentUtility {

    // Capturing Documents
    public static final int CAMERA_INTENT_CODE = 0;
    public static final int GALLERY_INTENT_CODE = 1;

    // Documents
    public static final String SITUATION_KEY = "sitch";
    public static final String PICKUP_VAL = "pickup";
    // Intent Flags

    // DOC TYPES
    // docType Options
    public static final String BOL_TYPE_KEY = "bill_lading";
    public static final String BOL_TYPE = "Bill of Lading";
    // Shipment options
    public static final String BOL_FILE_KEY = "bill_lading_path";
    // Driver options
    public static final String W9_KEY = "w9";
    public static final String W9_TYPE = "W9";
    public static final String INSURANCE_KEY = "insurance";
    public static final String INSURANCE_TYPE = "Insurance";
    public static final String VALID_AUTH_KEY = "valid_authority";
    public static final String VALID_AUTH_TYPE = "Valid Authority";
    public static final String IFTA_KEY = "ifta";
    public static final String IFTA_TYPE = "IFTA";
    public static final String IRP_KEY = "irp";
    public static final String IRP_TYPE = "IRP";
    // If we want to support licenses
    public static final String LICENSE_KEY = "drivers_license";
    public static final String LICENSE_TYPE = "License";
}
