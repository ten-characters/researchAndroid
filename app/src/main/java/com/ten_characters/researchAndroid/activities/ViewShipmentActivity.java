package com.ten_characters.researchAndroid.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.map.DirectionParser;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.ServerRequestTask;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import org.json.JSONObject;

import java.util.LinkedHashMap;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ViewShipmentActivity extends ActionBarActivity implements OnMapReadyCallback{
    private static final String LOG_TAG = ViewShipmentActivity.class.getSimpleName();

    private GoogleMap mMap;
    private Polyline mTripLine;
    private Marker startMarker, endMarker;
    private Shipment mShipment;


    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_shipment);

        mShipment = (Shipment) getIntent().getSerializableExtra(GeneralUtility.SHIPMENT_INTENT_KEY);

        if (mShipment.isPlaceholder()) {
            // Shouldn't even get here!
            // Finally can use the wtf level
            Log.wtf(LOG_TAG, "Trying to view a placeholder shipment?!");
            finish();
            return;
        }

        MapView mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(null);
        mapView.getMapAsync(this);

        setupInfoViews();
    }

    private void setupInfoViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        // Compile the lists
        LinkedHashMap<String,String> referenceMap = mShipment.getRefNumbers();
        LinearLayout refContainer = (LinearLayout)findViewById(R.id.reference_numbers_container);
        for (String key : referenceMap.keySet()) {
            View newRow = inflater.inflate(R.layout.list_row, null);
            ((TextView)newRow.findViewById(R.id.row_title)).setText(key);
            ((TextView)newRow.findViewById(R.id.row_content)).setText(referenceMap.get(key));
            refContainer.addView(newRow);
        }

        LinkedHashMap<String,String> infoMap = mShipment.getInfoHashMap();
        LinearLayout infoContainer = (LinearLayout)findViewById(R.id.info_container);
        for (String key : infoMap.keySet()) {
            View newRow = inflater.inflate(R.layout.list_row, null);
            ((TextView)newRow.findViewById(R.id.row_title)).setText(key);
            ((TextView)newRow.findViewById(R.id.row_content)).setText(infoMap.get(key));
            infoContainer.addView(newRow);
        }

        // Put an option to see the bill of lading if it exists!
        if (!mShipment.getBolPath().equals("") && mShipment.getBolPath() != null) {
            (findViewById(R.id.view_bol_button)).setVisibility(View.VISIBLE);
        }
    }

    public void onButtonClick(View view) {
        // Launch an image view intent!
        Intent imageviewIntent = new Intent(ViewShipmentActivity.this, ViewImageActivity.class);
        imageviewIntent.putExtra(GeneralUtility.FILENAME_INTENT_KEY, mShipment.getBolPath());
        startActivity(imageviewIntent);
    }

    /* SECTION */
    /* Map Stuff */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(mMap != null) {
            // SETUP DA MAP
            // Setup the bounds of the map
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(mShipment.getPickupLatLng());
            boundsBuilder.include(mShipment.getDropoffLatLng());
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 50; // px from edge of map
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

            drawRoute(mShipment.getPickupLatLng(), mShipment.getDropoffLatLng());
        }
    }

    private void drawRoute(LatLng start, LatLng end) {
        // Put down the start and end markers!
        startMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_start_pin))
                .position(start));
        endMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_end_flag_checkered))
                .position(end));


        // Draw the route on the lil map!
        final DirectionParser directionParser = new DirectionParser(this, mMap);

        String requestUrl = ServerUtility.getGoogleDirectionsUriString(start, end);

        ServerRequestTask directionsRequest = new ServerRequestTask(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(JSONObject result) {
                directionParser.parseInBackground(result);
            }
        });
        directionsRequest.execute(requestUrl, "GET");
    }

}
