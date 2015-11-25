package com.ten_characters.researchAndroid;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.SaveCallback;
import com.ten_characters.researchAndroid.server.OnFileTaskCompleted;
import com.ten_characters.researchAndroid.services.BackFromNavService;
import com.ten_characters.researchAndroid.services.FetchUnacceptedService;
import com.ten_characters.researchAndroid.services.TrackingService;
import com.ten_characters.researchAndroid.userInfo.User;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * First thing that's launched ... I think ...
 * DO base setups here, ie ParsePush initialization
 * Created by austin on 30/05/15.
 */

public class GlobalApp extends Application {

    private static final String LOG_TAG = GlobalApp.class.getSimpleName();
    private User currentUser = new User();
    private Shipment currentShipment = new Shipment();
    private PalletServer mServer;
    private Tracker mAnalyticsTracker;

    private OnTaskCompleted listener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            if (result != null) {
                try {
                    User reloadedUser = new User(getApplicationContext(), result.getJSONObject(ServerUtility.USER_KEY));
                    // Only set the new user if the reload was without exceptions
                    setCurrentUser(reloadedUser);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "No user data in reload response!");
                }
            }
        }
    };

    public GlobalApp() {}
    // Where we initialize the Parse Push Notifications
    @Override
    public void onCreate() {
        super.onCreate();

        // Let us first check if there is an active internet connection
        // That is needed!
        if (! GeneralUtility.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "You need to be connected to the internet to use Pallet!", Toast.LENGTH_LONG).show();
        }

        // Load our custom font
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/PalletFont.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        // We are going to try crash reporting with Parse as well
        //ParseCrashReporting.enable(this);

        // SETUP DIFFERENTLY FOR DEBUG / PRODUCTION
        // Todo: build some of this depending on the buildType / flavor
        Crashlytics crashlytics;
        if (BuildConfig.DEBUG) {
            Parse.initialize(this,
                    getString(R.string.parse_application_id_debug),
                    getString(R.string.parse_client_id_debug)
            );

            // Disable Crashlytics
            crashlytics = new Crashlytics.Builder()
                    .core(new CrashlyticsCore.Builder().disabled(true).build())
                    .build();

            // Set Google Analytics to a Dry Run. No fake Data!
            GoogleAnalytics.getInstance(this).setDryRun(true);
        } else {
            // Production Keys
            Parse.initialize(this,
                    getString(R.string.parse_application_id_production),
                    getString(R.string.parse_client_id_production)
            );

            // Setup Crashlytics normally
            crashlytics = new Crashlytics();
        }
        Fabric.with(this, crashlytics);

        // Setup parse!
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground("allInstalls", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "Subscribed to allInstalls!");
                } else {
                    Log.e("com.parse.push", "Failed to subscribe to allInstalls!", e);
                }
            }
        });

        mServer = new PalletServer(this, listener);

        startFetchingUnaccepted();
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mAnalyticsTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(1800);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mAnalyticsTracker = analytics.newTracker(R.xml.main_tracker);
            mAnalyticsTracker.enableAutoActivityTracking(true);
            mAnalyticsTracker.setUseSecure(true);
        }
        return mAnalyticsTracker;
    }


    // lol never even called on a real phone so silly Austin
    /*@Override
    public void onTerminate() {
        super.onTerminate();
        // Only stop tracking if they are not on a shipment!
        if (!hasShipment())
            stopTracking();
        // Unless do we want to keep pulling shipments in the background?
        // No! Let's use a sync adapter for this?
        stopFetchingUnaccepted();
        // Make sure they are of duty
    }*/

    public User getCurrentUser() {
        return currentUser;
    }

    public Shipment getCurrentShipment() {
        return currentShipment;
    }

    public boolean isPickedUp() {
        return currentShipment.isInTransit();
    }
    public void setIsPickedUp(boolean isPickedUp) {
        currentShipment.setIsInTransit(isPickedUp);
    }

    public boolean hasShipment() {
        return !currentShipment.isPlaceholder();
    }

    public void finishShipment() {
        // Todo: Check if they have any other shipments and then set the next as current?
        currentUser.removeShipment(currentShipment);

        if (currentUser.getCurrentShipments().length == 0) {
            // if they have no more current shipments, set a placeholder
            currentShipment = new Shipment();
        }
        // Could pass the shipment to be finished and then move the shipment to the list
        // finished shipments in the user
        // For now we will do nothing, maybe reload the user?
    }

    /** ----------------- CURRENT USER FUNCTIONS ----------------- */
    public void setCurrentUser(User user) {
        if (user.getNumShipments() != 0) {
            // This must mean that the driver has active shipments!
            // Get the current shipments now!
            if (user.getCurrentShipments().length > 0) {
                // For now just set the first current shipment
                setCurrentShipment(user.getCurrentShipments()[0]);
            }
        }

        currentUser = user;
        subscribeToChannels(user.getPushChannels());

        // Download all relevant and frequently accessed photos!
        if (user.hasProfilePicture()) {
            PalletServer downloadServer
                    = new PalletServer(getApplicationContext(), new OnFileTaskCompleted() {
                @Override
                public void onFileTaskCompleted(File result) {
                    GeneralUtility.saveProfilePicture(getApplicationContext(), result);
                }
            });
            downloadServer.downloadFile(user.getProfilePicPath());
        }

        // Specific analytics tracking!
        // GOOGLE ANALYTICS

        // CRASHLYTICS
        Crashlytics.getInstance().core.setUserIdentifier(user.getId());
        Crashlytics.getInstance().core.setUserEmail(user.getEmail());
        Crashlytics.getInstance().core.setUserName(user.getDisplayName());
    }

    public void reloadUser() {
        mServer.getCurrentUser();
    }
    public void voidUser() {
        // Stop all the services that could be running in the background
        stopAllServices();
        // Make sure to delete all user information locally
        // stored when they log out or it could be susceptible to others
        // not that many accounts should be using the same phone
        GeneralUtility.deleteProfilePicture(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().remove(ServerUtility.AUTH_TOKEN_KEY).apply();
        unsubscribeToChannels(currentUser.getPushChannels());
        currentUser = new User();

        // Void all the specific analytics
        Crashlytics.getInstance().core.setUserIdentifier("");
        Crashlytics.getInstance().core.setUserEmail("");
        Crashlytics.getInstance().core.setUserName("");
    }

    public void setCurrentShipment(Shipment shipment) {
        // Mark as on shipment!
        startTracking();
        currentShipment = shipment;
        currentShipment.setIsPlaceholder(false);
    }
    public void voidShipment() {
        currentShipment = new Shipment();
    }

    /* SECTION */
    /* SERVICES */
    public void stopAllServices() {
        stopService(new Intent(this, BackFromNavService.class));
        stopService(new Intent(this, FetchUnacceptedService.class));
        stopService(new Intent(this, TrackingService.class));
    }
    public void startBackNavService() {
        stopService(new Intent(this, BackFromNavService.class));
        startService(new Intent(this, BackFromNavService.class));
    }
    public void stopBackNavService() {
        stopService(new Intent(this, BackFromNavService.class));
    }

    public void startFetchingUnaccepted() {
        // We absolutely only want one of these services running at a time
        // stopService will just return false if there is no shipment running
        // see @ source code
        stopService(new Intent(this, FetchUnacceptedService.class));
        startService(new Intent(this, FetchUnacceptedService.class));
    }
    public void stopFetchingUnaccepted() {
        stopService(new Intent(this, FetchUnacceptedService.class));
    }

    // Create a better was to find if the tracker is already running
    public void startTracking() {
        stopService(new Intent(this, TrackingService.class));
        if (!isTracking()) {
           // Only turn on the service if their is not already one running!
           startService(new Intent(this, TrackingService.class));
        }
    }
    public void stopTracking() {
        stopService(new Intent(this, TrackingService.class));
    }
    // Todo: Simplify into one: private boolean isServiceRunning(Service service) {}
    private boolean isTracking() {
        boolean isTracking = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TrackingService.class.getName().equals(service.service.getClassName())) {
                isTracking = true;
            }
        }
        return isTracking;
    }

    public void subscribeToChannels(ArrayList<String> channels) {
        for (final String channel : channels) {
            ParsePush.subscribeInBackground(channel, new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Log.d("com.parse.push", "Subscribed to " + channel + "!");
                    } else {
                        Log.e("com.parse.push", "Failed to subscribe to " + channel + "!", e);
                    }
                }
            });
        }
    }

    public void unsubscribeToChannels(ArrayList<String> channels) {
        for (final String channel : channels) {
            ParsePush.unsubscribeInBackground(channel, new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Log.d("com.parse.push", "Unsubscribed to " + channel + "!");
                    } else {
                        Log.e("com.parse.push", "Failed to unsubscribe to " + channel + "!", e);
                    }
                }
            });
        }
    }
}