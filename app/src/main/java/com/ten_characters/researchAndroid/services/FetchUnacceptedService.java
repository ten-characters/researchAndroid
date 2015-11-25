package com.ten_characters.researchAndroid.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.ten_characters.researchAndroid.userInfo.Address;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.*;

import static com.ten_characters.researchAndroid.data.PalletDbContract.UnacceptedShipmentEntry.buildUnacceptedShipmentsUri;

/**
 * Created by austin on 5/07/15.
 */
public class FetchUnacceptedService extends Service {

    private static final String LOG_TAG = FetchUnacceptedService.class.getSimpleName();

    private PalletServer mServer;
    private Timer mTimer;
    private final long UPDATE_INTERVOL = 5 * 60 * 1000; // UPDATE EVERY 5 MINUTES; // MILLISECONDS

    private OnTaskCompleted listener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            if (result == null) return;
            try {
                if (result.has(ServerUtility.ERROR_CODE_KEY)) {
                    return;
                }
                // Parse the JSON into shipments and then write them to the database
                ArrayList<Shipment> shipments = new ArrayList<>();
                JSONArray shipmentsJSON = new JSONArray(result.getString(ServerUtility.SHIPMENTS_KEY));
                for (int i = 0; i < shipmentsJSON.length(); i++) {
                    shipments.add(new Shipment(shipmentsJSON.getJSONObject(i)));
                }

                // Apparently lists take up twice the space as arrays
                // so let's git it out of here as fast as possible
                Shipment[] shipmentsArr = new Shipment[shipments.size()];
                shipments.toArray(shipmentsArr);

                writeShipmentsToDb(shipmentsArr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem with server response JSON!", e);
            } catch (GeneralUtility.DateException e) {
                Log.e(LOG_TAG, "Problem parsing unaccepted shipments!", e);
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        mServer = new PalletServer(getApplicationContext(), listener);
        mTimer = new Timer();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchShipments();
        return START_STICKY;
    }

    private void fetchShipments() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mServer.getUnacceptedShipments();
            }
        }, 0, UPDATE_INTERVOL);
    }

    /* why are we double parsing and not just adding straight from the json returned?
     * Perhaps silly, but trying to parse helps provide data validation as well as putting it into
      * to handle formats, like our custom data classes
      * */
    public void writeShipmentsToDb(Shipment[] shipments) {
        ContentValues[] values = new ContentValues[shipments.length];

        for (int i = 0; i < values.length; i++) {
            ContentValues newShipment = new ContentValues();
            Shipment toAdd = shipments[i];
            newShipment.put(COLUMN_SHIPMENT_OBJ_ID, toAdd.getId());
            newShipment.put(COLUMN_PRICE, toAdd.getPrice());
            newShipment.put(COLUMN_COMMODITY, toAdd.getCommodity());
            newShipment.put(COLUMN_WEIGHT, toAdd.getWeight());
            newShipment.put(COLUMN_IS_FULL_TRUCKLOAD, toAdd.isFullTruckload());
            /*newShipment.put(COLUMN_IS_AVAILABLE, toAdd.isAvailable());
            newShipment.put(COLUMN_IS_AVAILABLE_IN_SECONDS, toAdd.getIsAvailableInSeconds());*/
            newShipment.put(COLUMN_NUM_PALLETS, toAdd.getNumPallets());
            newShipment.put(COLUMN_NUM_PIECES_PER_PALLET, toAdd.getNumPiecesPerPallet());
            newShipment.put(COLUMN_REF_NUMBERS_STRING, toAdd.getRefNumbersString());

            newShipment.put(COLUMN_PICKUP_NAME, toAdd.getPickupName());
            newShipment.put(COLUMN_PICKUP_RATING, toAdd.getPickupRating());
            newShipment.put(COLUMN_PICKUP_LAT, toAdd.getPickupLat());
            newShipment.put(COLUMN_PICKUP_LNG, toAdd.getPickupLng());
            newShipment.put(COLUMN_PICKUP_TIME, toAdd.getPickupTime().toString());
            newShipment.put(COLUMN_PICKUP_TIME_END, toAdd.getPickupTimeEnd().toString());
            newShipment.put(COLUMN_PICKUP_CONTACT_CSV, toAdd.getPickupContact().toCommaDelimitedString());

            // Todo: compress into single string and then parse when read ?
            Address pickupAddress = toAdd.getPickupAddress();
            newShipment.put(COLUMN_PICKUP_CITY, pickupAddress.getCity());
            newShipment.put(COLUMN_PICKUP_STATE, pickupAddress.getState());
            newShipment.put(COLUMN_PICKUP_COUNTRY, pickupAddress.getCountry());
            newShipment.put(COLUMN_PICKUP_ZIP, pickupAddress.getZip());


            newShipment.put(COLUMN_DROPOFF_NAME, toAdd.getDropoffName());
            newShipment.put(COLUMN_DROPOFF_RATING, toAdd.getDropoffRating());
            newShipment.put(COLUMN_DROPOFF_LAT, toAdd.getDropoffLat());
            newShipment.put(COLUMN_DROPOFF_LNG, toAdd.getDropoffLng());
            newShipment.put(COLUMN_DROPOFF_TIME, toAdd.getDropoffTime().toString());
            newShipment.put(COLUMN_DROPOFF_TIME_END, toAdd.getDropoffTimeEnd().toString());
            newShipment.put(COLUMN_DROPOFF_CONTACT_CSV, toAdd.getDropoffContact().toCommaDelimitedString());

            Address dropoffAddress = toAdd.getDropoffAddress();
            newShipment.put(COLUMN_DROPOFF_CITY, dropoffAddress.getCity());
            newShipment.put(COLUMN_DROPOFF_STATE, dropoffAddress.getState());
            newShipment.put(COLUMN_DROPOFF_COUNTRY, dropoffAddress.getCountry());
            newShipment.put(COLUMN_DROPOFF_ZIP, dropoffAddress.getZip());

            newShipment.put(COLUMN_NEEDS_LIFT, toAdd.needsLiftgate());
            newShipment.put(COLUMN_NEEDS_LUMPER, toAdd.needsLumper());
            newShipment.put(COLUMN_NEEDS_JACK, toAdd.needsJack());

            newShipment.put(COLUMN_RANGE_MILES, GeneralUtility.milesBetween(
                    GeneralUtility.getCurrentLatLng(getApplicationContext()),
                    toAdd.getPickupLatLng()));
            values[i] = newShipment;
        }

        // Now bulk insert all these thingers
        Uri shipmentUri = buildUnacceptedShipmentsUri();
        int inserted = getApplicationContext().getContentResolver().bulkInsert(shipmentUri, values);
        Log.d(LOG_TAG, "Inserted " + inserted + " shipments to DB!");
    }

}
