package com.ten_characters.researchAndroid.activities;



import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.maps.SupportMapFragment;
import com.ten_characters.researchAndroid.FavoritesDrawerHandler;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.NavigationDrawerHandler;
import com.ten_characters.researchAndroid.map.MainMapHandler;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.userInfo.Shipment;
import com.ten_characters.researchAndroid.userInfo.User;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/** APP TODO:
        Consider using less globals. Perhaps not the best practice. Let's talk to people.
       Don't allow non-drivers to sign in just yet ** Find a better way to do this

       In this activity:
            Determine what state we are in, free of shipment, on the way to pickup, on the way to dropoff
            and then do everything that needs doing i.e. setup buttons, start trackers, (zoom map -> in handler) !

*/

public class MainActivity extends ActionBarActivity implements View.OnClickListener, OnTaskCompleted {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static FragmentManager fragmentManager;
    public SupportMapFragment mapFragment;
    private NavigationDrawerHandler mNavigationDrawerHandler;
    private FavoritesDrawerHandler mFavoritesDrawerHandler;
    private MainMapHandler mMapHandler;
    private View mStatusIndicator;

    private FrameLayout mainFrame;

    private Button actionButton;
    private ImageButton backToNavButton;
    private ImageView shipmentResourcesButton;

    private ActionBar mActionBar;

    private PalletServer mServer;
    private GlobalApp app;
    // Keys for saving the state of the application
    private static final String STATE_CURRENT_SHIPMENT_KEY = "current_shipment";
    private static final String STATE_PICKED_UP_KEY = "picked_up";
    private static final int PICKUP_BOL_REQUEST = 1;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = ((GlobalApp) getApplication());

        if( savedInstanceState == null ) {
            setupActivity();
        } else {
            // Can we delete this yet? todo?
            if (savedInstanceState.containsKey(STATE_CURRENT_SHIPMENT_KEY)) {
                if (savedInstanceState.getSerializable(STATE_CURRENT_SHIPMENT_KEY) != null) {
                    app.setCurrentShipment((Shipment) savedInstanceState.getSerializable(STATE_CURRENT_SHIPMENT_KEY));
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNavigationDrawerHandler != null)
            mNavigationDrawerHandler.recycleProfileBitmap();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Try to add the profile picture! Like when navigating back from profile!
        mNavigationDrawerHandler.setProfilePicture(GeneralUtility.getProfilePictureFile(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if we have a situational intent and provide appropriate action1

        // Intent checker! A test if it works better here than in on start
        Intent intent = getIntent();
        if (intent.hasExtra(getResources().getString(R.string.nav_back_key))) {
            app.stopBackNavService();
            getIntent().removeExtra(getResources().getString(R.string.nav_back_key));
        }
        // Sent back from the signed document activity upon completion
        else if(intent.hasExtra(GeneralUtility.FINISHED_SHIPMENT_INTENT_KEY)) {
            // Launch a prompt to rate em!
            Intent rateIntent = new Intent(MainActivity.this, RatingActivity.class);
            rateIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY,
                    getIntent().getSerializableExtra(GeneralUtility.SHIPMENT_INTENT_KEY));
            startActivity(rateIntent);
        } else if (intent.hasExtra(GeneralUtility.FINISHED_RATING_INTENT_KEY)) {
            // We are really done with the shipment now!
            app.stopTracking();
            app.finishShipment();
            app.reloadUser();
        }

        // Todo: stop fuckin' hackin' 'round
        if (actionButton != null)
            setupButtons();
    }

    private void setupActivity() {
        // First check if the Current User is a driver. If not, send to login screen
        // And Alert them that only drivers can be signed in right now
        if (!app.getCurrentUser().isATrucker()) {
            Intent loginIntent = new Intent(MainActivity.this, AuthActivity.class);
            // Todo: send this as a flag or intent, don't make the Toast here!
            Toast.makeText(this, "Sorry, only Drivers may use the app at this time.", Toast.LENGTH_LONG);
            startActivity(loginIntent);
            return;
        }
        else if (!app.getCurrentUser().paymentIsConfirmed()) {
            /*Toast.makeText(this, "Payment method is not confirmed! " +
                    "\nIf you have not already added your method, please do so!" +
                    "\n <- In the Side Drawer!", Toast.LENGTH_LONG).show();*/
        }

        // Start the shipment gathering service to display unaccepted shipments
//        app.startFetchingUnaccepted();

        // Reloads drawer and map if the activity refreshes
        // Initialize DrawerList
        // Using this NavigationDrawerHandler to avoid doing *any* drawer config in here
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.container);

        mainFrame = (FrameLayout) findViewById(R.id.main_frame);

        // Setup custom mActionBar
        mActionBar = getSupportActionBar();
        mActionBar.setCustomView(R.layout.actionbar_home);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_HOME_AS_UP);

        ((mActionBar.getCustomView()).findViewById(R.id.action_bar_button)).setOnClickListener(this);

        // Setup the navigation drawers
        RecyclerView navRecycler = (RecyclerView) findViewById(R.id.main_nav_drawer);
        mNavigationDrawerHandler = new NavigationDrawerHandler(this,
                                                drawerLayout,
                                                navRecycler,
                                                app.getCurrentUser());

//        RecyclerView favRecycler = (RecyclerView) findViewById(R.id.favorite_shipments_drawer);
//        mFavoritesDrawerHandler = new FavoritesDrawerHandler(this, getSupportLoaderManager(), drawerLayout, favRecycler);

        // Get the Map Fragment from the Layout
        fragmentManager = getSupportFragmentManager();
        mapFragment = (SupportMapFragment) fragmentManager.findFragmentById(R.id.map_fragment);

        // Hands all map events off of the main activity code if the fragment was obtained successfully
        if (mapFragment != null) {
            mMapHandler = new MainMapHandler(mapFragment, this, getSupportLoaderManager());
        }

        // Setup the server and attach the listener
        mServer = new PalletServer(this, this);

        // Dynamically add the pickup / dropoff button if they are on a shipment
        // Set the button's height to 15% of the
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int buttonHeight = (int) (size.y * .15);

        actionButton = (Button) findViewById(R.id.action_button);
        actionButton.setOnClickListener(this);
        actionButton.setHeight(buttonHeight);

        backToNavButton = (ImageButton) findViewById(R.id.back_to_nav_button);
        backToNavButton.setOnClickListener(this);

//        shipmentResourcesButton = (ImageView) findViewById(R.id.shipment_resources_button);
//        shipmentResourcesButton.setOnClickListener(this);

        // Position the status indicator
        mStatusIndicator = findViewById(R.id.duty_status_indicator);
        // Totally lame austin
        mStatusIndicator.setX(128);
        setupButtons();
    }

    public void setupButtons() {
        /** Where we determine the state of the driver! ie available for freight, en route to shipment, in transit, whatever
         * Could add state to the Global User for easier transactions*/
        if (!app.hasShipment()) {
            // Only show the issue button if they are currently on a shipment
            // Sorry if they break down otherwise
            // Maybe in the future we offer them support?
            mStatusIndicator.setVisibility(View.VISIBLE);

            actionButton.setId(R.id.action_button);
            // add the toggle duty button the the view
            User user = app.getCurrentUser();
            if (user.isOnDuty()) {
                // Start tracking them if they are on duty!
                app.startTracking();
                actionButton.setText(R.string.on_duty);
                mStatusIndicator.setBackground(getResources().getDrawable(R.drawable.status_indicator_on));
            } else {
                // Make sure the tracking service is off if they are!
                app.stopTracking();
                actionButton.setText(R.string.off_duty);
                mStatusIndicator.setBackground(getResources().getDrawable(R.drawable.status_indicator_off));
            }
        } else {
            mStatusIndicator.setVisibility(View.GONE);
            // Just to make sure the tracking service is going
            // So we don't lose the lil suckers
            app.startTracking();

            // Setup the shipment resources button and the back to navigation button
            backToNavButton.setVisibility(View.VISIBLE);
//            shipmentResourcesButton.setVisibility(View.VISIBLE);

            // Move the map appropriately
            // To do this we need to set up a listener to animate only when
            // the view has been fully inflated
            // Todo: called twice at creation --> bad (?)
            // This is because we call setup buttons from both setup activity and onStart
            final View mapView = mapFragment.getView();
            if (mapView != null) {
                if (mapView.getViewTreeObserver().isAlive())
                    mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            mMapHandler.animateAround(app.getCurrentShipment());
                        }
                    });
            }

            if (!app.isPickedUp()) {
                // add the pickup button to the view
                // We will add flags to know if we've picked up or not too
                actionButton.setId(R.id.pickup_button);
                actionButton.setText(R.string.pickup);

            } else if (app.isPickedUp()) {
                // Indicates a dropoff!
                actionButton.setId(R.id.dropoff_button);
                actionButton.setText(R.string.dropoff);
            }
        }
    }

    @Override
    public void onClick(View button) {
        switch(button.getId()) {
            case R.id.action_button:
                // Toggles the application fields in the response
                mServer.toggleDuty();
                break;
            case R.id.pickup_button:
                // Send to camera activity with a pickup intent
                // Will send pickup request later
                Intent pickupIntent = new Intent(MainActivity.this, PickupActivity.class);
                pickupIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, app.getCurrentShipment());
                startActivityForResult(pickupIntent, PICKUP_BOL_REQUEST);
                // Start navigation to the pickup location
                break;
            case R.id.dropoff_button:
                Intent signIntent = new Intent(MainActivity.this, SignActivity.class);
                // Send the signing activity the image path to download
                // Hopefully this will make it easier to support multiple shipments later on
                // Will replace current shipment with a specific shipment when we get that far
                signIntent.putExtra(ServerUtility.SHIPMENT_KEY, app.getCurrentShipment());
                startActivity(signIntent);
                break;
            case R.id.back_to_nav_button:
                launchNav();
                break;
            /*case R.id.shipment_resources_button:
                Intent resourcesIntent = new Intent(MainActivity.this, ShipmentResourcesActivity.class);
                resourcesIntent.putExtra(ServerUtility.SHIPMENT_KEY, app.getCurrentShipment());
                startActivity(resourcesIntent);
                break;*/
            case R.id.action_bar_button:
                SoundPool sp = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);

                int soundId = sp.load(this, R.raw.truck_horn, 1);
                sp.play(soundId, 1, 1, 0, 0, 1);
                MediaPlayer mp = MediaPlayer.create(this, R.raw.truck_horn);
                mp.start();
                break;
        }
    }


    /** This is primarily used for returning from the BOL after taking a picture and uploading successfully */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Todo: Try to consolidate all routing from here!
        if (requestCode == PICKUP_BOL_REQUEST) {
            if (resultCode == RESULT_OK) {
                launchNav();
            }
        }
    }

    private void launchNav() {
        app.startBackNavService();

        double lat, lng;
        Shipment currentshipment = app.getCurrentShipment();

        if (currentshipment.isInTransit()) {
            // Aka has already been picked up
            lat = currentshipment.getDropoffLat();
            lng = currentshipment.getDropoffLng();
        } else {
            lat = currentshipment.getPickupLat();
            lng = currentshipment.getPickupLng();
        }
        // This means that the driver successfully uploaded the bol upon pickup
        // Now we want to route him to his final destination
        // ooooooo
        Uri navUri = Uri.parse("google.navigation:q="
                + Double.toString(lat) + ","
                + Double.toString(lng)
                + "&mode=d");

        Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
        startActivity(navIntent);
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        if (result != null) {
            // Could be different depending on which button is active
            switch (actionButton.getId()) {
                case R.id.action_button:
                    // Upon successful request, set global user!
                    // Just a quick toggle between booleans, don't get confuzzeld!
                    app.getCurrentUser().setIsActive(!app.getCurrentUser().isOnDuty());
                    if (app.getCurrentUser().isOnDuty()) {
                        app.startTracking();
                        actionButton.setText(getText(R.string.on_duty));
                        mStatusIndicator.setBackground(getResources().getDrawable(R.drawable.status_indicator_on));
                    } else {
                        app.stopTracking();
                        actionButton.setText(getText(R.string.off_duty));
                        mStatusIndicator.setBackground(getResources().getDrawable(R.drawable.status_indicator_off));
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (mNavigationDrawerHandler.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        // Can cause nullPointerException when Restoring State
        if(mNavigationDrawerHandler != null)
            mNavigationDrawerHandler.syncTogState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save all relevant shipment data if the driver is on a shipment
        if (app.hasShipment()) {
            outState.putSerializable(STATE_CURRENT_SHIPMENT_KEY, app.getCurrentShipment());
            if (app.isPickedUp()) {
                outState.putBoolean(STATE_PICKED_UP_KEY, true);
            } else {
                outState.putBoolean(STATE_PICKED_UP_KEY, false);
            }
        }
        // Must always call the super class
        super.onSaveInstanceState(outState);
    }
}
