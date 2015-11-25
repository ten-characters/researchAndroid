package com.ten_characters.researchAndroid.userInfo;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.ten_characters.researchAndroid.data.PalletDbContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ten_characters.researchAndroid.GeneralUtility.HEADER_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.ALLTIME_HEADER;
import static com.ten_characters.researchAndroid.GeneralUtility.ALLTIME_MOVES_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.ALLTIME_PROFIT_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.MOVES_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.PROFIT_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.WEEK_HEADER;
import static com.ten_characters.researchAndroid.GeneralUtility.WEEK_MOVES_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.WEEK_PROFIT_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.YEAR_HEADER;
import static com.ten_characters.researchAndroid.GeneralUtility.YEAR_MOVES_KEY;
import static com.ten_characters.researchAndroid.GeneralUtility.YEAR_PROFIT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.*;
import static com.ten_characters.researchAndroid.GeneralUtility.DateException;

/**
 * A template for all users
 * Must be serializable for security purposes, encodes all class data
 * Created by austin on 1/06/15.
 */


public class User implements Serializable{

    private static final String LOG_TAG = User.class.getSimpleName();
    // Everything relevant about the current user
    // Kept in variable storage so that it is destroyed when the application is destroyed
    // Should be queried every relaunch from server
    private String id;
    private String customer_id;
    private String email;
    private String firstName, roadName = "", lastName;
    private String phoneNumber;
    private Date dob;
    private Address billingInfo;
    private String company;
    private float rating;
    private String userType;
    private ArrayList<Shipment> shipments = new ArrayList<>(), finishedShipments = new ArrayList<>();
    private ArrayList<Truck> trucks = new ArrayList<>();
    private ArrayList<Trailer> trailers = new ArrayList<>();
    private double[] location;
    private String insurance_path = "", ifta_path = "", irp_path = "",
            license_path = "", profilePicPath = "";

    private String notifKey;
    private String referralCode;

    private boolean isActive, paymentConfirmed, isATrucker = true;
    private ArrayList<String> pushChannels = new ArrayList<>();

    private ArrayList<String> favoriteShipments = new ArrayList<>();

    // For setting up later
    public User() {}

    // For setting up NOW!
    public User( Context context, JSONObject data) {
        try {
            id = data.getJSONObject(ID_KEY).getString(OBJ_ID_KEY);
            try {
                customer_id = data.getString(CUSTOMER_ID_KEY);
            } catch (JSONException e) {
                customer_id = "temp_client";
            }
            notifKey = data.getString(NOTIF_KEY);
            referralCode = data.getString(REFERRAL_CODE_KEY);
            email = data.getString(EMAIL_KEY);
            firstName = data.getString(FIRST_NAME_KEY);
            lastName = data.getString(LAST_NAME_KEY);
            company = data.getString(COMPANY_KEY);
            phoneNumber = data.getString(PHONE_KEY);
            userType = data.getString(USER_TYPE_KEY);

            // Get and sort shipments!
            JSONArray shipmentArr = data.getJSONArray(SHIPMENTS_KEY);
            for (int i = 0; i < shipmentArr.length(); i++) {
                Shipment shipment = new Shipment(shipmentArr.getJSONObject(i));
                shipments.add(shipment);
            }
            JSONArray finishedArr = data.getJSONArray(FINISHED_SHIPMENTS_KEY);
            for (int i = 0; i < finishedArr.length(); i++) {
                Shipment shipment = new Shipment(finishedArr.getJSONObject(i));
                finishedShipments.add(shipment);
            }

            rating = (float) data.getDouble(RATING_KEY);

            billingInfo = new Address(data.getJSONObject(BILLING_INFO_KEY));

            if (data.has(DRIVER_INFO_KEY)) {
                JSONObject driverInfo = data.getJSONObject(DRIVER_INFO_KEY);
                isActive = driverInfo.getBoolean(IS_ACTIVE_KEY);

                JSONArray favShipmentsArr = driverInfo.getJSONArray(FAVORITES_KEY);
                // Clear the table upon reload
                context.getContentResolver().delete(PalletDbContract.FavoriteShipmentEntry.buildFavoriteShipmentsUri(), null, null);
                for (int i = 0; i < favShipmentsArr.length(); i++) {
                    addToFavorites(context, favShipmentsArr.getJSONObject(i).getString(OBJ_ID_KEY));
                }

                paymentConfirmed = driverInfo.getBoolean(PAYMENT_CONFIRMED_KEY);
                insurance_path = driverInfo.getString(INSURANCE_KEY);
                license_path = driverInfo.getString(LICENSE_KEY);

                // Add the equipment !
                JSONArray trucksJSON = driverInfo.getJSONArray(TRUCKS_KEY), trailersJSON = driverInfo.getJSONArray(TRAILERS_KEY);
                for (int i = 0; i < trucksJSON.length(); i++)
                    trucks.add(new Truck(trucksJSON.getJSONObject(i)));

                for (int i = 0; i < trailersJSON.length(); i++)
                    trailers.add(new Trailer(trailersJSON.getJSONObject(i)));

            } else {
                Log.d(LOG_TAG, "No driver info.. admin?");
                isATrucker = false;
            }
            double[] location = {data.getJSONArray(LOCATION_KEY).getDouble(0),
                    data.getJSONArray(LOCATION_KEY).getDouble(1)};
            this.location = location;

            // THE KEY is just an added level of protection
            pushChannels.add("KEY" + notifKey);
            pushChannels.add("base_user");
            pushChannels.add(userType);

            profilePicPath = data.getString(PROFILE_PATH_KEY);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to set up user from JSON!", e);
        } catch (DateException e) {
            Log.e(LOG_TAG, "Failed to parse dates!", e);
        }
    }

    // ALL GETTERS
    public LinkedHashMap<String, String> getProfileInfoHashMap() {

        double totalProfit = 0.0, yearProfit = 0.0, weekProfit = 0.0;
        int yearMoves = 0, weekMoves = 0;
        Date oneWeekBack = new Date((new Date()).getTime() - 604800000L); // 7L * 24L * 60L * 60L * 1000L;
        Date oneYearBack = new Date((new Date()).getTime() - 525600000L); // 365L * 24L * 60L * 60L * 1000L;
        for (Shipment shipment: finishedShipments) {
            double price = Double.parseDouble(shipment.getPrice());
            totalProfit += price;

            if (shipment.getTimeFinished().after(oneWeekBack)) {
                weekProfit += price;
                weekMoves++;
            }

            if (shipment.getTimeFinished().after(oneYearBack)) {
                yearProfit+= price;
                yearMoves++;
            }
        }

        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        toReturn.put(WEEK_MOVES_KEY, Integer.toString(weekMoves));
        toReturn.put(WEEK_PROFIT_KEY, "$" + String.format("%.2f", weekProfit));
        toReturn.put(YEAR_MOVES_KEY, Integer.toString(yearMoves));
        toReturn.put(YEAR_PROFIT_KEY, "$" + String.format("%.2f", yearProfit));
        toReturn.put(ALLTIME_MOVES_KEY, Integer.toString(finishedShipments.size()));
        toReturn.put(ALLTIME_PROFIT_KEY, "$" + String.format("%.2f", totalProfit));

        return toReturn;
    }

    /** This is just to get a nice lil list that can be queried by position to build the profile cards*/
    public List<Map<String, String>> getProfileInfoArrayList() {
        // Set a static capacity, this will take up less room
        // Change this is we want to add more info to the card
        List<Map<String, String>> profInfo = new ArrayList<Map<String, String>>(3) {};

        double totalProfit = 0.0, yearProfit = 0.0, weekProfit = 0.0;
        int yearMoves = 0, weekMoves = 0;
        Date oneWeekBack = new Date((new Date()).getTime() - 604800000L); // 7L * 24L * 60L * 60L * 1000L;
        Date oneYearBack = new Date((new Date()).getTime() - 525600000L); // 365L * 24L * 60L * 60L * 1000L;
        for (Shipment shipment: finishedShipments) {
            double price = Double.parseDouble(shipment.getPrice());
            totalProfit += price;

            if (shipment.getTimeFinished().after(oneWeekBack)) {
                weekProfit += price;
                weekMoves++;
            }

            if (shipment.getTimeFinished().after(oneYearBack)) {
                yearProfit+= price;
                yearMoves++;
            }
        }

        // three cards: week, year, all time
        // Should contain: header, moves, profit
        // Could store profits in like an array and then cycle through but with only three cards, this seems easier
        Map<String, String> weekMap = new HashMap<>(3), yearMap = new HashMap<>(3), alltimeMap = new HashMap<>(3);
        // Headers
        weekMap.put(HEADER_KEY, WEEK_HEADER);
        yearMap.put(HEADER_KEY, YEAR_HEADER);
        alltimeMap.put(HEADER_KEY, ALLTIME_HEADER);
        // Moves
        weekMap.put(MOVES_KEY, Integer.toString(weekMoves));
        yearMap.put(MOVES_KEY, Integer.toString(yearMoves));
        alltimeMap.put(MOVES_KEY, Integer.toString(finishedShipments.size()));
        // Profit
        weekMap.put(PROFIT_KEY, "$" + String.format("%.2f", weekProfit));
        yearMap.put(PROFIT_KEY, "$" + String.format("%.2f", yearProfit));
        alltimeMap.put(PROFIT_KEY, "$" + String.format("%.2f", totalProfit));
        // add them to the list to return!
        profInfo.add(weekMap);
        profInfo.add(yearMap);
        profInfo.add(alltimeMap);

        return profInfo;
    }

    public List<LinkedHashMap<String, String>> getEquipmentInfoArrayList() {
        List<LinkedHashMap<String, String>> equipList = new ArrayList<LinkedHashMap<String, String>>() {};

        for (Truck truck: trucks)
            equipList.add(truck.getInfoMap());

        for (Trailer trailer: trailers)
            equipList.add(trailer.getInfoMap());

        return equipList;
    }

    public void addToFavorites(Context context, String shipmentId) {
        // Use the content provider!
        // It's kinda helpful but mainly it should be a super good loader method
        // Will consider removing the array lists
        favoriteShipments.add(shipmentId);
        ContentValues toAdd = new ContentValues();
        toAdd.put(PalletDbContract.FavoriteShipmentEntry.COLUMN_SHIPMENT_OBJ_ID, shipmentId);
        context.getContentResolver().insert(
                PalletDbContract.FavoriteShipmentEntry.buildFavoriteShipmentsUri(),
                toAdd);
    }
    public void removeFromFavorites(Context context, String shipmentId) {
        favoriteShipments.remove(shipmentId);
        context.getContentResolver().delete(
                PalletDbContract.FavoriteShipmentEntry.buildFavoriteWithObjIdUri(shipmentId), null, null);
    }
    public boolean isAFavorite(String shipmentId) {
        return favoriteShipments.contains(shipmentId);
    }

    public Truck getFirstTruck() {
        if (trucks.size() != 0)
            return trucks.get(0);
        return null;
    }

    public Trailer getFirstTrailer() {
        if(trailers.size() != 0)
            return trailers.get(0);
        return null;
    }

    public boolean isATrucker() {
        return isATrucker;
    }

    public int getNumShipments() { return shipments.size(); }

    /** Todo: transfer this to use xgliff ? */
    public String getDisplayName() {
        return firstName + " " + "\"" + roadName + "\"" + " " + lastName;
    }

    public String getPromoCode() {
        return referralCode;
    }

    public float getRating() {
        return rating;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getId(){ return id; }

    public String getEmail() {
        return email;
    }

    public boolean hasProfilePicture() {
        return !profilePicPath.equals("");
    }
    public String getProfilePicPath() {
        return profilePicPath;
    }
    public void setProfilePicPath(String profilePicPath) {
        this.profilePicPath = profilePicPath;
    }

    public String getNotifKey() {
        return notifKey;
    }
    public ArrayList<String> getPushChannels() {
        return pushChannels;
    }

    public Boolean isOnDuty() {
        return isActive;
    }

    public Shipment[] getCurrentShipments() {
        List<Shipment> shipList = new ArrayList<>();
        for (Shipment shipment: shipments) {
            if (shipment.isActive())
                shipList.add(shipment);
        }

        Shipment[] toReturn = new Shipment[shipList.size()];
        shipList.toArray(toReturn);
        return toReturn;
    }
    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }
    public void removeShipment(Shipment shipment) { shipments.remove(shipment); }

    public ArrayList<Shipment> getPastShipmentsArrList() {
        return finishedShipments;
    }

    public Shipment[] getPastShipmentsArr() {
        Shipment[] toReturn = new Shipment[finishedShipments.size()];
        finishedShipments.toArray(toReturn);
        return toReturn;
    }

    public boolean paymentIsConfirmed() { return paymentConfirmed; }
    public boolean isEmpty() {
        // Everyone has an id. Seems like a good measurement.
        if (id == null) {
            return true;
        }
        return false;
    }

}
