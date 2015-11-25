package com.ten_characters.researchAndroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import com.ten_characters.researchAndroid.activities.OfferShipmentActivity;
import com.ten_characters.researchAndroid.activities.SplashActivity;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import java.util.Date;

/**
 * Receives and routes all push notifications that come in from the Parse service
 *
 * Created by austin on 30/05/15.
 */
public class PushNotificationHandler extends ParsePushBroadcastReceiver {

    private static final String LOG_TAG = PushNotificationHandler.class.getSimpleName();
    private static final String PARSE_DATA_KEY = "com.parse.Data";
    private static final String PARSE_TYPE_KEY = "type";

    private static final String PARSE_ALERT_KEY = "alert";
    private static final String PALLET_MESSAGE_KEY = "message";
    private static final String PALLET_LINK_KEY = "link";
    private static final String PALLET_OFFER_SHIPMENT_KEY = "offer_shipment";

    public static final String PALLET_OFFER_SHIPMENT_DATA_KEY = "offer_data";

    // SHITTY WORKAROUND : DON'T JUDGE --> FIX!
    private static JSONObject lastPushReceived = new JSONObject();

    /** Determine if the Push is an offered shipment or an alert
     * if alert: display notification
     * if not: do nothing as we will display the offer screen
     * */
    @Override
    protected Notification getNotification(Context context, Intent intent) {
        JSONObject data = getDataFromIntent(intent);
        Notification notification = null;

        // Decipher if it is a parse website push or a custom Pallet Push!


        try {
            NotificationCompat.Builder notifBuilder;
            // Create a pendingIntent as a oh so not far off hope for our Pallet
            PendingIntent pendingIntent;
            if (data.has(PARSE_ALERT_KEY)) {
                // A parse alert!
                notifBuilder = new NotificationCompat.Builder(context)
                                .setSmallIcon(R.mipmap.ic_pallet_logo)
                                .setContentTitle("Pallet")
                                .setContentText(data.getString(PARSE_ALERT_KEY))
                                .setDefaults(Notification.DEFAULT_VIBRATE)
                                .setDefaults(Notification.DEFAULT_LIGHTS)
                                .setAutoCancel(false);

                Intent launchIntent = new Intent(context, SplashActivity.class);
                pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
            } else {
                // A custom Pallet one!
                // For now this will just a shipment!
                notifBuilder = new NotificationCompat.Builder(context)
                                .setSmallIcon(R.mipmap.ic_pallet_logo)
                                .setContentTitle("Shipment Offer!")
                                .setContentText(data.getString(PARSE_ALERT_KEY))
                                .setDefaults(Notification.DEFAULT_VIBRATE)
                                .setDefaults(Notification.DEFAULT_LIGHTS)
                                .setAutoCancel(false);



                if(data.has(PALLET_LINK_KEY)) {
                    // If we want to link to the Pallet website or other..
                    Intent linkIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(data.getString(PALLET_LINK_KEY)));
                    pendingIntent = PendingIntent.getActivity(context, 0, linkIntent, 0);
                } else {
                    // Otherwise just an intent to start the main activity of pallet
                    Intent launchIntent = new Intent(context, SplashActivity.class);
                    pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
                }
            }

            notifBuilder.setContentIntent(pendingIntent);
            notification = notifBuilder.build();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem creating Push Notification.", e);
        }

        // Only returns null if there was a problem OR
        // if the notification type is not alert
        return notification;
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {
        super.onPushOpen(context, intent);
    }

    /** Determine if the Push is an offered shipment
     * if so: send to offer activity
     * if not: do nothing
     * */
    @Override
    protected void onPushReceive(Context context, Intent intent) {
        // Log.v(LOG_TAG, "Received the push notification!");

        JSONObject data = getDataFromIntent(intent);

        // A workaround for accidental double shipment offers
        if (data.equals(lastPushReceived))
            return;
        lastPushReceived = data;

        String type;
        try {
            // Only want to launch activity if it pertains to
            type = data.getString(PARSE_TYPE_KEY);
            Log.d(LOG_TAG, "The push notification type is : " + type);
            switch (type){
                case PALLET_OFFER_SHIPMENT_KEY:
                    // Get the Json data object containing all the good offer info
                    JSONObject offerData = data.getJSONObject(PALLET_OFFER_SHIPMENT_DATA_KEY);

                    // Create an intent to launch the Offer Shipment Screen
                    Intent offerIntent = new Intent(context, OfferShipmentActivity.class);
                    offerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Attach all relevant data about the offer to construct the
                    Shipment offeredShipment = new Shipment(offerData.getJSONObject("shipment"));
                    // Since we're not technically on the main thread, let's parse all the data here
                    offerIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, offeredShipment);

                    // Attach the expiration ! For now we are just using the expiration_secs
                    // because time_utc is annoying
                    // hehe
                    final String EXPIRATION_KEY = "expiration";
                    final String EXPIRATION_SECS_KEY = "expiration_secs";

                    long millisToExpiration = offerData.getLong(EXPIRATION_SECS_KEY) * 1000;
                    //expiration = parseDate(offerJSONData.getString(EXPIRATION_KEY));
                    Date expiration = new Date();
                    expiration.setTime(expiration.getTime() + millisToExpiration);
                    offerIntent.putExtra(GeneralUtility.EXPIRATION_INTENT_KEY, expiration);

                    Log.d(LOG_TAG, "Starting Offer Shipment Activity!");
                    try {
                        context.getApplicationContext().startActivity(offerIntent);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Couldn't start activity!");
                    }
                    break;
                default:
                    Log.e(LOG_TAG, "Push notification with unmapped type: " + type);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Got a Parse alert Push notification!", e);
            super.onPushReceive(context, intent);
        } catch (GeneralUtility.DateException e) {
            Log.e(LOG_TAG, "Problem with the dates from push notification!", e);
        }
    }

    public JSONObject getDataFromIntent(Intent intent){
        JSONObject data;
        try {
            data = new JSONObject(intent.getExtras().getString(PARSE_DATA_KEY));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "The Push JSON was not readable.", e);
            return null;
        }
        return data;
    }

    
}
