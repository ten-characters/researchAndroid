package com.ten_characters.researchAndroid.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.ten_characters.researchAndroid.auth.AccountUtility.saveToken;

/**
 * Created by austin on 26/06/15.
 */
public class TrackingService extends Service implements SensorEventListener, LocationListener {

    private static final String LOG_TAG = TrackingService.class.getSimpleName();

    private PalletServer server;
    private LocationManager locManager;
    private Location currentLocation;
    private static final long UPDATE_INTERVOL = 120000; //2 * 60 * 1000; // MILLISECONDS // EVERY TWO MINUTES
    private int UPDATE_RANGE_SENSITIVITY; // METERS
    private String currentProvider;

    // Sensor to get the orientation!
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private static float[] accelerometerValues = new float[3], magnetValues = new float[3];
    private static Double orientation;

    private Timer timer;
    private Date lastSent;

    private OnTaskCompleted serverListener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            try {
                if (result == null) { throw new JSONException("No result"); }
                if (result.has(ServerUtility.NEW_TOKEN_KEY)) {
                    server.invalidateAuthToken();
                    saveToken(getApplicationContext(), result.getString(ServerUtility.NEW_TOKEN_KEY));
                }
                if (result.has(ServerUtility.ERROR_CODE_KEY)) {
                    switch (result.getString(ServerUtility.ERROR_CODE_KEY)) {
                        case "403":
                            // Stop the updating service
                            stopSelf();
                            break;
                    }
                } else {
                    Log.d(LOG_TAG, "Successfully Updated!");
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem with server response JSON!", e);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        server = new PalletServer(getApplicationContext(), serverListener);
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        timer = new Timer();
        UPDATE_RANGE_SENSITIVITY = 150; // Less than .1 miles

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Register for all those orientation updates!
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        // And now for what we've all been waiting for .. the Location Updates! ! !
        boolean gpsEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean passiveEnabled = locManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        if (!gpsEnabled) {
            Toast.makeText(getApplicationContext(), "Please turn on your GPS services!", Toast.LENGTH_LONG).show();
        }
        else if (!networkEnabled) {
            Toast.makeText(getApplicationContext(), "Please turn on your network location services!", Toast.LENGTH_LONG).show();
        }
        else if (!passiveEnabled) {
            Toast.makeText(getApplicationContext(), "Please turn on location services!", Toast.LENGTH_LONG).show();
        }

        // Try to get the last known location from the service running, starting with gps and then cascading
        // Checks gps first and then tries all others if we can't get it
        if (gpsEnabled) {
            currentLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, UPDATE_RANGE_SENSITIVITY, this);
            currentProvider = LocationManager.GPS_PROVIDER;
        }
        if (networkEnabled && currentLocation == null) {
            currentLocation = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, UPDATE_RANGE_SENSITIVITY, this);
            currentProvider = LocationManager.NETWORK_PROVIDER;
        }
        if (passiveEnabled && currentLocation == null) {
            currentLocation = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            locManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, UPDATE_RANGE_SENSITIVITY, this);
            currentProvider = LocationManager.PASSIVE_PROVIDER;
        }

        if (currentLocation == null) {
            // If we still can't get there location, we really shouldn't be allowing them to provide service
        } else {
            updateLocation(currentLocation);
        }

        // Start this service not sticky so that it doesn't get cancelled even
        // if the app is shut down
        // Can't be losing truckers mid shipment now can we!
        return START_STICKY_COMPATIBILITY;
    }

    private void updateLocation(final Location location) {
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Ensure that not too many updates are sent
                    if (location != null) {
                        // Its totes OK if we send the same location over and over
                        // so long as it is indeed their current location
                        if (lastSent == null || (new Date().getTime()) - lastSent.getTime() >= UPDATE_INTERVOL - 100) {
                            lastSent = new Date();
                            server.updateLocation(location, orientation, location.getSpeed());
                            Log.d(LOG_TAG, "Updating location!");
                        }
                    }
                }
            }, 0, UPDATE_INTERVOL);
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "The timer was already canceled!", e);
        }
    }

    // SECTION
    // Location
    @Override
    public void onLocationChanged(Location location) {
        // Cancel the current updating timer and start a new one
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            // If the GPS is ever enabled we definately want to use that!
            locManager.removeUpdates(this);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, UPDATE_RANGE_SENSITIVITY, this);
            currentProvider = provider;
        } else if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && provider.equals(LocationManager.NETWORK_PROVIDER)) {
            // If the next best enable is the
            locManager.removeUpdates(this);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, UPDATE_RANGE_SENSITIVITY, this);
            currentProvider = provider;
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getBaseContext(), "Please turn on your GPS services!", Toast.LENGTH_LONG).show();
        }

        if (provider.equals(currentProvider)) {
            currentProvider = locManager.getBestProvider(new Criteria(), true);
            locManager.removeUpdates(this);
            locManager.requestLocationUpdates(currentProvider, 0, UPDATE_RANGE_SENSITIVITY, this);
        }
    }

    // SECTION
    // Orientation

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            accelerometerValues = event.values;
        }
        else if (event.sensor == magnetometer) {
            magnetValues = event.values;
        }
        // Fancy math, see @ Google Android Dev documentation for SensorManger
        float[] matrixR = new float[9], matrixI = new float[9];
        float[] matrixValues = new float[3];
        boolean success = SensorManager.getRotationMatrix(matrixR, matrixI, accelerometerValues, magnetValues);
        if (success) {
            SensorManager.getOrientation(matrixR, matrixValues);
            // This is the Azimuth, the celestial degrees.. or something. idk. Take astrology.
            orientation = Math.toDegrees(matrixValues[0]);
        } else {
            orientation = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        locManager.removeUpdates(this);
        sensorManager.unregisterListener(this);

        super.onDestroy();
    }


}
