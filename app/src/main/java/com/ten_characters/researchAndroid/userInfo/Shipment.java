package com.ten_characters.researchAndroid.userInfo;

import android.database.Cursor;

import com.google.android.gms.maps.model.LatLng;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.data.PalletDbContract;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;

import static com.ten_characters.researchAndroid.GeneralUtility.intToBool;
import static com.ten_characters.researchAndroid.GeneralUtility.parseDate;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_COMMODITY;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_CITY;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_CONTACT_CSV;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_COUNTRY;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_LAT;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_LNG;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_NAME;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_RATING;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_STATE;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_TIME_END;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_DROPOFF_ZIP;
//import static com.truckpallet.pallet.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_IS_AVAILABLE;
//import static com.truckpallet.pallet.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_IS_AVAILABLE_IN_SECONDS;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_IS_FULL_TRUCKLOAD;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_NUM_PALLETS;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_NUM_PIECES_PER_PALLET;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_CITY;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_CONTACT_CSV;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_COUNTRY;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LAT;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LNG;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_NAME;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_RATING;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_STATE;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_TIME;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_TIME_END;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_ZIP;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_PRICE;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_REF_NUMBERS_STRING;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.COLUMN_WEIGHT;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME;
import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry._ID;
import static com.ten_characters.researchAndroid.server.ServerUtility.BOL_PATH_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.COMMODITY_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_ADDR_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_CONTACT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_LOC_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_NAME_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_RATING_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_TIME_END_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.DROPOFF_TIME_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.ID_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.IS_FINISHED_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.IS_FULL_TRUCKLOAD_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.IS_IN_TRANSIT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NEEDS_JACK_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NEEDS_LIFT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NEEDS_LUMP_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NUM_PALLETS_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NUM_PIECES_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.OBJ_ID_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_ADDR_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_CONTACT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_LOC_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_NAME_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_RATING_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_TIME_END_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PICKUP_TIME_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PRICE_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.REFERENCE_NUMS_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.TIME_FINISHED_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.WEIGHT_KEY;

/**
 * Created by austin on 26/06/15.
 * For now we will use this to store the current shipment, but perhaps later it can be implemented
 * in a profile page
 */
public class Shipment implements Serializable {
    private String id;
    private String price;
    private String commodity;
    private double weight;
    private boolean isFullTruckload;
    private int numPallets, numPiecesPerPallet;

    private LinkedHashMap<String,String> refNumbers = new LinkedHashMap<>();

    private String pickupName;
    private double[] pickupLocation = new double[2];
    private Address pickupAddress;
    private Date pickupTime, pickupTimeEnd;
    private String dropoffName;
    private double[] dropoffLocation = new double[2];
    private Address dropoffAddress;
    private Date dropoffTime, dropoffTimeEnd;

    private Contact pickupContact, dropoffContact;

    private boolean needsLiftgate;
    private boolean needsLumper;
    private boolean needsJack;
    private float pickupRating, dropoffRating;
    private String bolPath;

   /* private boolean isAvailable;
    private double isAvailableInSeconds;*/

    private boolean isInTransit = false;
    private boolean isFinished;
    private Date timeFinished;
    // Just for our use so we can pass around a shipment even if there isn't one
    // This alerts us that there isn't one. Yay.
    private boolean isPlaceholder = true;

    public Shipment(){}
    public Shipment(JSONObject data) throws JSONException, GeneralUtility.DateException{
        try {
            id = data.getJSONObject(ID_KEY).getString(OBJ_ID_KEY);
        } catch (JSONException e) {
            id = data.getString(ID_KEY);
        }
        price = data.getString(PRICE_KEY);
        commodity = data.getString(COMMODITY_KEY);
        weight = data.getDouble(WEIGHT_KEY);
        isFullTruckload = data.getBoolean(IS_FULL_TRUCKLOAD_KEY);
        numPallets = data.getInt(NUM_PALLETS_KEY);
        numPiecesPerPallet = data.getInt(NUM_PIECES_KEY);

        JSONObject refs = data.getJSONObject(REFERENCE_NUMS_KEY);
        Iterator<String> refKeys = refs.keys();
        while (refKeys.hasNext()) {
            String key = refKeys.next();
            String value = refs.getString(key);
            // Make sure the value is valid
            // @craig, send nice values please
            if(!value.equals(""))
                refNumbers.put(key, value);
        }

        pickupName = data.getString(PICKUP_NAME_KEY);
        pickupRating = (float) data.getDouble(PICKUP_RATING_KEY);
        double[] tempPickup = {data.getJSONArray(PICKUP_LOC_KEY).getDouble(0),
                data.getJSONArray(PICKUP_LOC_KEY).getDouble(1)};
        pickupLocation = tempPickup;
        pickupAddress = new Address(data.getJSONObject(PICKUP_ADDR_KEY));
        pickupTime = parseDate(data.getString(PICKUP_TIME_KEY));
        pickupTimeEnd = parseDate(data.getString(PICKUP_TIME_END_KEY));

        pickupContact = new Contact(data.getJSONObject(PICKUP_CONTACT_KEY));

        dropoffName = data.getString(DROPOFF_NAME_KEY);
        dropoffRating = (float) data.getDouble(DROPOFF_RATING_KEY);
        double[] tempDropoff = {data.getJSONArray(DROPOFF_LOC_KEY).getDouble(0),
                data.getJSONArray(DROPOFF_LOC_KEY).getDouble(1)};
        dropoffLocation = tempDropoff;
        dropoffAddress = new Address(data.getJSONObject(DROPOFF_ADDR_KEY));
        dropoffTime = parseDate(data.getString(DROPOFF_TIME_KEY));
        dropoffTimeEnd = parseDate(data.getString(DROPOFF_TIME_END_KEY));

        dropoffContact = new Contact(data.getJSONObject(DROPOFF_CONTACT_KEY));

        needsLiftgate = data.getBoolean(NEEDS_LIFT_KEY);
        needsLumper = data.getBoolean(NEEDS_LUMP_KEY);
        needsJack = data.getBoolean(NEEDS_JACK_KEY);

        /*isAvailable = data.getBoolean(IS_AVAILABLE_KEY);
        isAvailableInSeconds = data.getDouble(IS_AVAILABLE_IN_SECONDS_KEY);*/

        try {
            isInTransit = data.getBoolean(IS_IN_TRANSIT_KEY);
        } catch (JSONException e) {
            isInTransit = false;
        }

        try {
            isFinished = data.getBoolean(IS_FINISHED_KEY);
            timeFinished = GeneralUtility.parseDate(data.getString(TIME_FINISHED_KEY));
        } catch (JSONException e) {
            isFinished = false;
        }

        if (data.has(BOL_PATH_KEY))
            bolPath = data.getString(BOL_PATH_KEY);
        else
            bolPath = "";

        isPlaceholder = false;
    }

    public LinkedHashMap<String, String> getInfoHashMap() {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        toReturn.put("Price: ", "$" + getPrice());
        toReturn.put("Commodity: ", commodity);
        toReturn.put("Weight: ", Double.toString(weight) + "lbs");
        toReturn.put("Number of Pallets: ", Integer.toString(numPallets));
        toReturn.put("Pieces per pallet: ", Integer.toString(numPiecesPerPallet));
        toReturn.put("Pickup: ", getOfferPickupAddress());
        toReturn.put("Dropoff: ", getOfferDropoffAddress());
        toReturn.put("Pickup Contact:", pickupContact.name);
        toReturn.put("Pickup number:", pickupContact.phone);
        toReturn.put("Dropoff Contact:", dropoffContact.name);
        toReturn.put("Dropoff number:", dropoffContact.phone);
        return toReturn;
    }

    public String getOfferDescription() {
        String description = "";
        if (isFullTruckload)
            description += "Truckload:\n";

        // Should use xml for this I guess
        description += numPallets + " pallets, " + this.getStringWeight() + " " + commodity;
        return description;
    }

    /** Return different time windows depending on if they are on the same day or not
     *  Make it pretty, customized, covergirl
     *  Should be on the same day. We will run into problems if this is not on the same day! */
    public String getTimeWindowString(Date windowStart, Date windowEnd) {
        String window, dateString;

        // Determine if it is today, tomorrow, or later
        // if later, just display the date in mm/dd/yy format
        Calendar todayMidnight = GregorianCalendar.getInstance();
        todayMidnight.setTime(new Date());
        todayMidnight.add(Calendar.DATE, 1);
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);

        // Trying this new thing called comparing with calendars
        // Supposedly its cool but I don't get the hype
        // Might go back to dates cuz lame
        Calendar tomorrowMidnight = GregorianCalendar.getInstance();
        tomorrowMidnight.setTime(new Date());
        tomorrowMidnight.add(Calendar.DATE, 2);
        tomorrowMidnight.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowMidnight.set(Calendar.MINUTE, 0);
        tomorrowMidnight.set(Calendar.SECOND, 0);
        tomorrowMidnight.set(Calendar.MILLISECOND, 0);

        Calendar compareCal = GregorianCalendar.getInstance();
        // First check if the date is today
        compareCal.setTime(windowStart);
        if (compareCal.before(todayMidnight)) {
            // Today !
            dateString = "Today: ";
        } else if (compareCal.before(tomorrowMidnight)) {
            // OO OO OO it's tomorrow !
            dateString = "Tomorrow: ";
        } else {
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
            dateString = df.format(windowStart);
        }

        SimpleDateFormat amFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        window = dateString + "\nFrom:" + amFormat.format(windowStart) + " - " + amFormat.format(windowEnd);
        return window;
    }

    public boolean isPlaceholder() {
        return isPlaceholder;
    }

    public void setIsPlaceholder(boolean isPlaceholder) {
        this.isPlaceholder = isPlaceholder;
    }

    public boolean isActive() {
        // lol
        return !isFinished;
        // lol
    }

    public boolean isInTransit() {
        return isInTransit;
    }
    public void setIsInTransit(boolean isInTransit) {
        this.isInTransit = isInTransit;
    }

    /*public boolean isAvailable() {
        return isAvailable;
    }
    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public double getIsAvailableInSeconds() {
        return isAvailableInSeconds;
    }
    public double getIsAvailableInHours() {
        double hours = isAvailableInSeconds % 3600;
        double remainder = (isAvailableInSeconds / 3600) - hours;
        return Math.round(hours + remainder);
    }
    public void setIsAvailableInSeconds(double isAvailableInSeconds) {
        this.isAvailableInSeconds = isAvailableInSeconds;
    }*/

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public double getWeight() {
        return weight;
    }
    public String getStringWeight() { return weight + "lbs."; }
    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getCommodity() {
        return commodity;
    }
    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public String getPrice() {
        double dPrice = Double.parseDouble(price);
        return String.format("%.2f", dPrice);
    }
    public double getDoublePrice() {
        return Double.parseDouble(price);
    }
    public void setPrice(String price) {
        this.price = price;
    }

    public boolean isFullTruckload() {
        return isFullTruckload;
    }
    public void setIsFullTruckload(boolean isFullTruckload) {
        this.isFullTruckload = isFullTruckload;
    }

    public int getNumPallets() {
        return numPallets;
    }
    public void setNumPallets(int numPallets) {
        this.numPallets = numPallets;
    }

    public int getNumPiecesPerPallet() {
        return numPiecesPerPallet;
    }
    public void setPiecesPerPallet(int numPiecesPerPallet) {
        this.numPiecesPerPallet = numPiecesPerPallet;
    }

    public String getPrimaryReferenceNumber() {
        // Stored layered by the primary reference key
        String primaryNumber = refNumbers.get(refNumbers.get(ServerUtility.PRIMARY_REF_KEY));
        // Support for now, shouldn't be a problem with a clean database
        // and an updated server
        if (primaryNumber == null)
            return "";
        return primaryNumber;
    }
    public String getRefNumbersString() {
        return refNumbers.toString();
    }
    public LinkedHashMap<String,String> getRefNumbers() {
        // Remove the primary key from the list, duh its not a display key
        refNumbers.remove(ServerUtility.PRIMARY_REF_KEY);
        return refNumbers;
    }
    public void setRefNumbers(String refString) {
        // Start by taking off the brackets
        refString = refString.substring(1, refString.length()-1);
        // Then split each pair by the comma-delimiter
        String[] kVPairs = refString.split(",");
        // Now let us match
        LinkedHashMap<String, String> newRefs = new LinkedHashMap<>();
        for(String pair: kVPairs) {
            // Split them between the equality!
            String[] entry = pair.split("=");
            // Trim them to make sure that they aren't fattened up
            newRefs.put(entry[0].trim(), entry[1].trim());
        }
        refNumbers = newRefs;
    }
    public void setRefNumbers(LinkedHashMap<String, String> refNumbers) {
        this.refNumbers = refNumbers;
    }

    public void setPickupName(String pickupName) {
        this.pickupName = pickupName;
    }
    public String getPickupName() {
        return pickupName;
    }

    public void setPickupRating(float pickupRating) {
        this.pickupRating = pickupRating;
    }
    public float getPickupRating() {
        return pickupRating;
    }

    public void setPickupTime(Date pickupTime) {
        this.pickupTime = pickupTime;
    }
    public Date getPickupTime() {
        return pickupTime;
    }
    public void setPickupTimeEnd(Date pickupTimeEnd) {
        this.pickupTimeEnd = pickupTimeEnd;
    }
    public Date getPickupTimeEnd() {
        return pickupTimeEnd;
    }
    public String getPickupTimeWindowString() {
        return getTimeWindowString(pickupTime, pickupTimeEnd);
    }

    public Double getPickupLat() { return pickupLocation[0]; }
    public Double getPickupLng() { return pickupLocation[1]; }
    public LatLng getPickupLatLng() { return new LatLng(pickupLocation[0], pickupLocation[1]); }

    public void setPickupAddress(Address pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
    public Address getPickupAddress() {
        return pickupAddress;
    }
    public String getOfferPickupAddress() {
        return pickupAddress.getOfferFormat();
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLocation[0] = pickupLat;
    }
    public void setPickupLng(double pickupLng) {
        this.pickupLocation[1] = pickupLng;
    }


    public void setDropoffAddress(Address dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }
    public Address getDropoffAddress() {
        return dropoffAddress;
    }
    public String getOfferDropoffAddress() {
        return dropoffAddress.getOfferFormat();
    }

    public String getDropoffName() {
        return dropoffName;
    }

    public void setDropoffRating(float dropoffRating) {
        this.dropoffRating = dropoffRating;
    }
    public float getDropoffRating() {
        return dropoffRating;
    }

    public void setDropoffTime(Date dropoffTime) {
        this.dropoffTime = dropoffTime;
    }
    public Date getDropoffTime() {
        return dropoffTime;
    }
    public void setDropoffTimeEnd(Date dropoffTimeEnd) {
        this.dropoffTimeEnd = dropoffTimeEnd;
    }
    public Date getDropoffTimeEnd() {
        return dropoffTimeEnd;
    }
    public String getDropoffTimeWindowString() {
        return getTimeWindowString(dropoffTime, dropoffTimeEnd);
    }

    public Double getDropoffLat() { return dropoffLocation[0]; }
    public Double getDropoffLng() { return dropoffLocation[1]; }
    public LatLng getDropoffLatLng() { return new LatLng(dropoffLocation[0], dropoffLocation[1]); }

    public void setDropoffName(String dropoffName) {
        this.dropoffName = dropoffName;
    }
    public void setDropoffLat(double dropoffLat) {
        this.dropoffLocation[0] = dropoffLat;
    }
    public void setDropoffLng(double dropoffLng) {
        this.dropoffLocation[1] = dropoffLng;
    }

    public Contact getPickupContact() {
        return pickupContact;
    }
    public void setPickupContact(Contact pickupContact) {
        this.pickupContact = pickupContact;
    }

    public Contact getDropoffContact() {
        return dropoffContact;
    }
    public void setDropoffContact(Contact dropoffContact) {
        this.dropoffContact = dropoffContact;
    }

    public boolean needsLumper() {
        return needsLumper;
    }
    public void setNeedsLumper(boolean needsLumper) {
        this.needsLumper = needsLumper;
    }

    public boolean needsJack() {
        return needsJack;
    }
    public void setNeedsJack(boolean needsJack) {
        this.needsJack = needsJack;
    }

    public boolean needsLiftgate() {
        return needsLiftgate;
    }
    public void setNeedsLiftgate(boolean needsLiftgate) {
        this.needsLiftgate = needsLiftgate;
    }

    public String getBolPath() { return bolPath; }
    public void setBolPath(String bolPath) {
        this.bolPath = bolPath;
    }

    public Date getTimeFinished() {
        return timeFinished;
    }


    /** All this is for loaders! It's a whole bunch of fun! */
    public static final String [] sObjIdProjectionWithLatLng = {
            PalletDbContract.UnacceptedShipmentEntry.TABLE_NAME + "." + PalletDbContract.UnacceptedShipmentEntry._ID,
            PalletDbContract.UnacceptedShipmentEntry.COLUMN_SHIPMENT_OBJ_ID,
            PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LAT,
            PalletDbContract.UnacceptedShipmentEntry.COLUMN_PICKUP_LNG
    };

    public static final String[] sFullProjection = {
            TABLE_NAME + "." + _ID,
            COLUMN_SHIPMENT_OBJ_ID,
            COLUMN_PRICE,
            COLUMN_COMMODITY,
            COLUMN_WEIGHT,
            COLUMN_IS_FULL_TRUCKLOAD,
            COLUMN_NUM_PALLETS,
            COLUMN_NUM_PIECES_PER_PALLET,
            COLUMN_REF_NUMBERS_STRING,

            COLUMN_PICKUP_NAME,
            COLUMN_PICKUP_RATING,
            COLUMN_PICKUP_LAT,
            COLUMN_PICKUP_LNG,
            COLUMN_PICKUP_TIME,
            COLUMN_PICKUP_TIME_END,

            COLUMN_PICKUP_CITY,
            COLUMN_PICKUP_STATE,
            COLUMN_PICKUP_COUNTRY,
            COLUMN_PICKUP_ZIP,

            COLUMN_DROPOFF_NAME,
            COLUMN_DROPOFF_RATING,
            COLUMN_DROPOFF_LAT,
            COLUMN_DROPOFF_LNG,
            COLUMN_DROPOFF_TIME,
            COLUMN_DROPOFF_TIME_END,

            COLUMN_DROPOFF_CITY,
            COLUMN_DROPOFF_STATE,
            COLUMN_DROPOFF_COUNTRY,
            COLUMN_DROPOFF_ZIP,

            COLUMN_PICKUP_CONTACT_CSV,
            COLUMN_DROPOFF_CONTACT_CSV,

            /*COLUMN_IS_AVAILABLE,
            COLUMN_IS_AVAILABLE_IN_SECONDS*/
    };

    /** Could think about just looking up the column by name in the query. That could be easier than this shit. Thanks Katherine. */

    public static final int COL_SHIPMENT_OBJ_ID = 1;
    public static final int COL_PRICE = 2;
    public static final int COL_COMMODITY = 3;
    public static final int COL_WEIGHT = 4;
    public static final int COL_IS_FULL_TRUCKLOAD = 5;
    public static final int COL_NUM_PALLETS = 6;
    public static final int COL_NUM_PIECES = 7;
    public static final int COL_REF_STRING = 8;

    public static final int COL_PICKUP_NAME = 9;
    public static final int COL_PICKUP_RATING = 10;
    public static final int COL_PICKUP_LAT = 11;
    public static final int COL_PICKUP_LNG = 12;
    public static final int COL_PICKUP_TIME = 13;
    public static final int COL_PICKUP_TIME_END = 14;

    public static final int COL_PICKUP_CITY = 15;
    public static final int COL_PICKUP_STATE = 16;
    public static final int COL_PICKUP_COUNTRY = 17;
    public static final int COL_PICKUP_ZIP = 18;

    public static final int COL_DROPOFF_NAME = 19;
    public static final int COL_DROPOFF_RATING = 20;
    public static final int COL_DROPOFF_LAT = 21;
    public static final int COL_DROPOFF_LNG = 22;
    public static final int COL_DROPOFF_TIME = 23;
    public static final int COL_DROPOFF_TIME_END = 24;

    public static final int COL_DROPOFF_CITY = 25;
    public static final int COL_DROPOFF_STATE = 26;
    public static final int COL_DROPOFF_COUNTRY = 27;
    public static final int COL_DROPOFF_ZIP = 28;

    public static final int COL_PICKUP_CONTACT = 29;
    public static final int COL_DROPOFF_CONTACT = 30;

    public static final int COL_IS_AVAILABLE = 31;
    public static final int COL_IS_AVAILABLE_IN_SECONDS = 32;
    
    
    public void setupFromFullProjection(Cursor data) throws GeneralUtility.DateException {
        // Moved here from MainMapHandler.java
        // Could be stripped of all these function calls
        // More time later date
        setId(data.getString(COL_SHIPMENT_OBJ_ID));
        setPrice(data.getString(COL_PRICE));
        setCommodity(data.getString(COL_COMMODITY));
        setWeight(data.getDouble(COL_WEIGHT));
        setIsFullTruckload(intToBool(data.getInt(COL_IS_FULL_TRUCKLOAD)));
        /*setIsAvailable(intToBool(data.getInt(COL_IS_AVAILABLE)));
        setIsAvailableInSeconds(data.getDouble(COL_IS_AVAILABLE_IN_SECONDS));*/

        setNumPallets(data.getInt(COL_NUM_PALLETS));
        setPiecesPerPallet(data.getInt(COL_NUM_PIECES));
        setRefNumbers(data.getString(COL_REF_STRING));

        setPickupName(data.getString(COL_PICKUP_NAME));
        setPickupRating(COL_PICKUP_RATING);
        setPickupLat(data.getDouble(COL_PICKUP_LAT));
        setPickupLng(data.getDouble(COL_PICKUP_LNG));
        setPickupContact(new Contact(data.getString(COL_PICKUP_CONTACT)));
        Address tempAddress = new Address(
                data.getString(COL_PICKUP_CITY),
                data.getString(COL_PICKUP_STATE),
                data.getString(COL_PICKUP_COUNTRY),
                data.getString(COL_PICKUP_ZIP)
        );
        setPickupAddress(tempAddress);

        setDropoffName(data.getString(COL_DROPOFF_NAME));
        setDropoffRating(data.getFloat(COL_DROPOFF_RATING));
        setDropoffLat(data.getDouble(COL_DROPOFF_LAT));
        setDropoffLng(data.getDouble(COL_DROPOFF_LNG));
        setDropoffContact(new Contact(data.getString(COL_DROPOFF_CONTACT)));
        tempAddress = new Address(
                data.getString(COL_DROPOFF_CITY),
                data.getString(COL_DROPOFF_STATE),
                data.getString(COL_DROPOFF_COUNTRY),
                data.getString(COL_DROPOFF_ZIP)
        );
        setDropoffAddress(tempAddress);

        // Format dates
        setPickupTime(GeneralUtility.parseDate(data.getString(COL_PICKUP_TIME)));
        setPickupTimeEnd(GeneralUtility.parseDate(data.getString(COL_PICKUP_TIME_END)));

        setDropoffTime(GeneralUtility.parseDate(data.getString(COL_DROPOFF_TIME)));
        setDropoffTimeEnd(GeneralUtility.parseDate(data.getString(COL_DROPOFF_TIME_END)));
    }
}
