package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.map.DirectionParser;
import com.ten_characters.researchAndroid.server.ServerRequestTask;
import com.ten_characters.researchAndroid.userInfo.Shipment;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONObject;

import java.util.Date;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class OfferShipmentActivity extends Activity implements OnMapReadyCallback, OnTaskCompleted {

    private static final String LOG_TAG = OfferShipmentActivity.class.getSimpleName();

    private Vibrator mVibrator;
    private static PalletServer mServer;
    private Shipment offeredShipment;
    private CountDownTimer expirationCountdown;
    private boolean wasAccepted, isInFavorites = false;
    private Button favoriteButton;

    private Date expiration;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    /** Populate with Shipment ListViews from the push notification data*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        offeredShipment = (Shipment) getIntent().getSerializableExtra(ServerUtility.SHIPMENT_KEY);

        if (getIntent().hasExtra(GeneralUtility.EXPIRATION_INTENT_KEY)) {
            expiration = (Date) getIntent().getSerializableExtra(GeneralUtility.EXPIRATION_INTENT_KEY);
        }

        mServer = new PalletServer(getApplicationContext(), this);
        // If we can't get the proper data, we must mark this as a bad request and reject it
        if (offeredShipment == null) {
            Log.e(LOG_TAG, "Couldn't get offered shipment from intent/push!");
            wasAccepted = false;
            sendResponse();
            // if the shipment is null, I guess we just have to let it time out on the server
            this.finish();
        }

        setContentView(R.layout.activity_offer_shipment);
        buildView();
    }

    public void buildView() {
        // Setup the lil map view
        MapView mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(null);
        mapView.getMapAsync(this);

        RecyclerView offerRecycler = (RecyclerView) findViewById(R.id.offer_recycler_view);
        offerRecycler.setHasFixedSize(true);
        offerRecycler.setLayoutManager(new LinearLayoutManager(this));
        offerRecycler.setAdapter(new OfferRecyclerAdapter(this, offeredShipment));

        // Differential between favoriting and being able to accept/reject
        FrameLayout buttonContainer = (FrameLayout) findViewById(R.id.offer_button_container);
        LinearLayout buttonsBar;

        // Weeding out all this favorite stuff, as our system is taking a step back from that
//        if (offeredShipment.isAvailable()) {
             buttonsBar = (LinearLayout) getLayoutInflater().inflate(R.layout.buttons_accept_reject, null, false);
        /*} else {
            buttonsBar = (LinearLayout) getLayoutInflater().inflate(R.layout.button_favorite, null, false);
            favoriteButton = (Button) buttonsBar.findViewById(R.id.offer_favorite_button);
            // Todo: rework this to search in the database, not through a list in the user
            // Defaults to 'Favorite'
            if (((GlobalApp) getApplication()).getCurrentUser().isAFavorite(offeredShipment.getId())) {
                // If it is already a toggleFavorite, change the text yo!
                favoriteButton.setText(R.string.offer_unfavorite);
                isInFavorites = true;
            }
        }*/
        buttonContainer.addView(buttonsBar);

        if (expiration != null) {
            // If everything has been populated without error we can start the countdown!
            // ...dun dun dun...
            startCountdown();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng start = offeredShipment.getPickupLatLng();
        LatLng end = offeredShipment.getDropoffLatLng();

        // Zoom to fit both end points
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        // Add the markers and such to the map!
        map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_start_pin))
                        .position(start)
        );
        boundsBuilder.include(start);

        map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_end_flag_checkered))
                        .position(end)
        );
        boundsBuilder.include(end);

        LatLng currentLatLng = GeneralUtility.getCurrentLatLng(this);
        if(currentLatLng != null) {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 40; // px from edge of map
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }

        // Draw the route on the lil map!
        final DirectionParser directionParser = new DirectionParser(this, map);

        String requestUrl = ServerUtility.getGoogleDirectionsUriString(start, end);

        ServerRequestTask directionsRequest = new ServerRequestTask(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(JSONObject result) {
                directionParser.parseInBackground(result);
            }
        });
        directionsRequest.execute(requestUrl, "GET");
    }

    /** Basically onButtonClickListener, sync button choice with server */
    public void onButtonClick(View button) {
        // Sort buttons and coordinate with server!
        // Make double checkskis that the user really wants to make this decision!
        switch (button.getId()) {
            case R.id.offer_accept_button:
                // Adding in a dialog to ensure that they meant it
                // Just another level of protection ya know
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Are you sure you want to accept?");
                alertBuilder.setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wasAccepted = true;
                        if (expiration != null) {
                            mVibrator.cancel();
                            expirationCountdown.cancel();
                        }
                        sendResponse();
                        // Nav will be started when the request finishes
                    }
                });
                alertBuilder.setNegativeButton("No..", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertBuilder.show();

                break;
            case R.id.offer_decline_button:
                if (expiration != null)
                    expirationCountdown.onFinish();
                finish();
                break;
            case R.id.offer_favorite_button:
                mServer.toggleFavorite(offeredShipment.getId());
                if (isInFavorites) {
                    ((GlobalApp) getApplication()).getCurrentUser().removeFromFavorites(this, offeredShipment.getId());
                    isInFavorites = false;
                    favoriteButton.setText(R.string.offer_favorite);
                }
                else {
                    ((GlobalApp) getApplication()).getCurrentUser().addToFavorites(this, offeredShipment.getId());
                    isInFavorites = true;
                    favoriteButton.setText(R.string.offer_unfavorite);
                }
                break;
            default:
                Log.e(LOG_TAG, "Unmapped button!");
        }
    }

    private void sendResponse() {
        mServer.answerShipmentOffer(wasAccepted, ((GlobalApp) getApplication()).getCurrentUser(), offeredShipment);
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        if (wasAccepted) {
            // Add the shipment to the users list of current shipments!
            ((GlobalApp) getApplication()).getCurrentUser().addShipment(offeredShipment);

            // Begin navigating to the start location
            Uri navUri = Uri.parse("google.navigation:q="
                    + Double.toString(offeredShipment.getPickupLat()) + ","
                    + Double.toString(offeredShipment.getPickupLng())
                    + "&mode=d");

            // set this shipment to be the current shipment
            ((GlobalApp) getApplication()).setCurrentShipment(offeredShipment);
            ((GlobalApp) getApplication()).startBackNavService();
            Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
            startActivity(navIntent);
            ((GlobalApp) getApplication()).setCurrentShipment(offeredShipment);
        }
    }

    /** Use the textfield to start a countdown on every minute.
     * Could vibrate and flash if we want to be flashy */
    private void startCountdown() {

        long millisExpiration = expiration.getTime();
        long millisUTC = System.currentTimeMillis();

        mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        long millisCountdown = millisExpiration - millisUTC;
        TextView countdownView = (TextView) findViewById(R.id.offer_countdown);
        // The countdown is gone if there is no expiration time!
        countdownView.setVisibility(View.VISIBLE);
        // Make a new countdown timer to alert the driver of the imminent doom
        // Send back to Main activity when the time is done
        expirationCountdown = offerCountdownTimer(millisCountdown, mVibrator, countdownView);
        expirationCountdown.start();
        // Must minimize offer if there is still time on the countdown
        // but exit the offer altogether idf the time runs out
    }

    /* SECTION */
    /* INNER CLASSES */
    private CountDownTimer offerCountdownTimer(final long millisToExpiration, Vibrator vibrator, TextView countdownView) {
        final long MILLIS = millisToExpiration;
        final TextView COUNTDOWN_VIEW = countdownView;
        final Vibrator VIBRATOR = vibrator;

        CountDownTimer timer = new CountDownTimer(millisToExpiration, 1000) {

            int totalMins = (int)MILLIS / 1000 / 60;

            @Override
            public void onTick(long millisUntilFinished) {
                // Update the countdown view
                long seconds = millisUntilFinished % 1000 % 60;
                if (seconds == 0)
                    totalMins--;

                /*if (seconds > 10)
                    COUNTDOWN_VIEW.setText("Expires in " + totalMins + ":" + seconds);
                else
                    COUNTDOWN_VIEW.setText("Expires in " + totalMins + ":0" + seconds);*/
                COUNTDOWN_VIEW.setText("Expires in " + millisUntilFinished / 1000 + " seconds");

                // Vibrate the device every 10 seconds
                if (seconds%10 == 0)
                    VIBRATOR.vibrate(500);
            }

            @Override
            public void onFinish() {
                VIBRATOR.cancel();
                cancel();
                wasAccepted = false;
                sendResponse();
            }
        };
        return timer;
    }

    private static class OfferRecyclerAdapter extends RecyclerView.Adapter<OfferRecyclerAdapter.OfferViewHolder> {

        private static final int TYPE_PRICE = 0, TYPE_LOCATION = 1;

        private final Context mContext;
        private final Shipment mShipment;
        private final LatLng currentLatLng;

        public OfferRecyclerAdapter(Context context, Shipment shipment) {
            mContext = context;
            mShipment = shipment;

            currentLatLng = GeneralUtility.getCurrentLatLng(context);
        }

        @Override
        public OfferViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_LOCATION)
                return new OfferViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_offer_body, parent, false), TYPE_LOCATION);
            return new OfferViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_offer_price, parent, false), TYPE_PRICE);
        }

        @Override
        public void onBindViewHolder(OfferViewHolder holder, int position) {
            if(getItemViewType(position) == TYPE_PRICE) {
                holder.shipmentText.setText(mShipment.getOfferDescription());
                holder.priceText.setText("$" + mShipment.getPrice());
            } else {
                // Determine if its a pickup or dropoff
                // Let's just do this simply for now: pos 1 == pickup / pos 2 == dropoff
                String header, location, timeWindow, distance;
                float rating;
                if (position == 1) {
                    header = mContext.getResources().getString(R.string.pickup_header);
                    location = mShipment.getOfferPickupAddress();
                    timeWindow = mShipment.getPickupTimeWindowString();
                    // Format the distance to only 1 decimal points
                    distance = String.format("%.1f", GeneralUtility.milesBetween(
                            currentLatLng,
                            mShipment.getPickupLatLng()
                    ));
                    rating = mShipment.getPickupRating();
                } else { //position == 2
                    // Todo: @us Should this be from the pickup? or from current location?
                    header = mContext.getResources().getString(R.string.dropoff_header);
                    location = mShipment.getOfferDropoffAddress();
                    timeWindow = mShipment.getDropoffTimeWindowString();
                    distance = String.format("%.1f", GeneralUtility.milesBetween(
                            currentLatLng,
                            mShipment.getDropoffLatLng()
                    ));
                    rating = mShipment.getDropoffRating();
                }

                // Set up all the views
                holder.headerText.setText(header);
                holder.locationText.setText(location);
                holder.timeWindowText.setText(timeWindow);
                holder.distanceText.setText("About " + distance + " miles away");
                holder.ratingBar.setRating(rating);
            }
        }

        @Override
        public int getItemCount() {
            // There should be three cards: price, pickup, dropoff
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            // The first card should be the price
            if (position == 0)
                return TYPE_PRICE;
            return TYPE_LOCATION;
        }

        public static class OfferViewHolder extends RecyclerView.ViewHolder {
            public TextView headerText;
            // For price
            public TextView shipmentText, priceText;
            // For body
            public TextView locationText, timeWindowText, distanceText;
            public RatingBar ratingBar;

            public OfferViewHolder(View itemView, int viewType) {
                super(itemView);
                headerText = (TextView) itemView.findViewById(R.id.offer_header_text);

                if (viewType == TYPE_PRICE) {
                    shipmentText = (TextView) itemView.findViewById(R.id.offer_shipment_text);
                    priceText = (TextView) itemView.findViewById(R.id.offer_price_text);
                } else {
                    locationText = (TextView) itemView.findViewById(R.id.offer_location_text);
                    timeWindowText = (TextView) itemView.findViewById(R.id.offer_time_window_text);
                    distanceText = (TextView) itemView.findViewById(R.id.offer_distance_text);
                    ratingBar = (RatingBar) itemView.findViewById(R.id.offer_rating_bar);
                }
            }
        }
    }
}
