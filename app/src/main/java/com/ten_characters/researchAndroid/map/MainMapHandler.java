package com.ten_characters.researchAndroid.map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.activities.OfferShipmentActivity;
import com.ten_characters.researchAndroid.data.PalletDbContract;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.ServerRequestTask;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import static com.ten_characters.researchAndroid.userInfo.Shipment.*;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by austin on 12/05/15.
 *
 * Todo:
 */
public class MainMapHandler implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener,
                                        GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationButtonClickListener,
                                            LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = MainMapHandler.class.getSimpleName();

    private Context mContext;
    private Activity mContainerActivity;

    private GoogleMap mMap;
    private Location mCurrentLocation;
    private DirectionParser mDirectionParser;

    private boolean markersVisible = true, isOnShipment = false; // Just so we don't always iterate through. I hope this is more efficient.
    private Marker endMarker, selectedMarker;
    private ArrayList<Marker> markerList = new ArrayList<>();
    private Shipment mSelectedShipment;
    private ArrayList<String> shipmenObjIdList = new ArrayList<>();
    private LoaderManager mLoaderManager;
    private Cursor mUnacceptedCursor;
    private static final Integer UNACCEPTED_LOADER_ID = 0;


    public MainMapHandler(MapFragment mapFragment, Context context,
                          LoaderManager loaderManager) {
        mContext = context;
        mLoaderManager = loaderManager;
        mContainerActivity = (Activity) mContext;
        mapFragment.getMapAsync(this);
        isOnShipment = ((GlobalApp)mContainerActivity.getApplication()).hasShipment();
    }
    public MainMapHandler(SupportMapFragment supportMapFragment, Context context,
                          LoaderManager loaderManager) {
        mContext = context;
        mLoaderManager = loaderManager;
        mContainerActivity = (Activity) mContext;
        supportMapFragment.getMapAsync(this);
        isOnShipment = ((GlobalApp)mContainerActivity.getApplication()).hasShipment();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(mMap != null) {
            setupMap();
        }
    }

    public void setupMap() {
        // This links us up so we can be notified about a change in data!
        mLoaderManager.initLoader(UNACCEPTED_LOADER_ID, null, this);

        // Create the direction parser
        mDirectionParser = new DirectionParser(mContext, mMap);

        // Get current Location
        mCurrentLocation = GeneralUtility.getCurrentLocation(mContext);

        // Zoom in on current location if we have successfully obtained a location
        if(mCurrentLocation != null) {
            LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(),
                                              mCurrentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 40));
            // Zoom , Milliseconds , callback
            mMap.animateCamera(CameraUpdateFactory.zoomTo(9), 3000, null);
        }

        //Move buttons nice and tight
        mMap.setPadding(0, 0, 0, 0);

        //Set Listeners
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        //For customer shipment infoWindows
        mMap.setInfoWindowAdapter(new ShipmentInfoWindowAdapter(mContainerActivity.getLayoutInflater()));

        //MAP settings
        mMap.setMyLocationEnabled(true);

        //UI settings
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        mMap.setTrafficEnabled(false);

        // If the trucker is on a shipment, draw the current route!
        if (isOnShipment) {
            GlobalApp app = (GlobalApp)mContainerActivity.getApplication();
            Shipment shipment = app.getCurrentShipment();
            //mMap.setTrafficEnabled(false);
            endMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_end_flag_checkered))
                    .position(app.getCurrentShipment().getDropoffLatLng()));

            // Draw the route to their next destination, depending on if they've picked it up or nah
            if (app.isPickedUp())
                drawRoute(shipment.getPickupLatLng(), shipment.getDropoffLatLng());
            else
                drawRoute(GeneralUtility.getCurrentLatLng(mContext), shipment.getPickupLatLng());

            // If on a shipment, markers should not be allowed to be clicked.
            // This is an easy way to avoid double accepting of freight
        }
    }

    public void animateAround(LatLng[] latLngs) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LatLng latLng: latLngs)
            boundsBuilder.include(latLng);

        LatLngBounds bounds = boundsBuilder.build();
        int padding = 190; // px from edge of map, large because map button overlay
        if (mMap != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    /** Determine whether on the way to pickup or on the way to dropoff,
     *  as this affects our zoom zoom */
    public void animateAround(Shipment shipment) {
        LatLng[] shipmentPoints = new LatLng[2];
        if (!shipment.isInTransit()) {
            shipmentPoints[0] = GeneralUtility.getCurrentLatLng(mContext);
            shipmentPoints[1] = shipment.getPickupLatLng();
        } else {
            shipmentPoints[0] = shipment.getPickupLatLng();
            shipmentPoints[1] = shipment.getDropoffLatLng();
        }
        animateAround(shipmentPoints);
    }

    /* SECTION */
    /* CLICK LISTENERS */

    @Override
    public boolean onMyLocationButtonClick() {
        // Zoom back to the current location
        // Also, just tryna find the best way to get current location
        // Seems easy enough with the map right here
        // mMap.getMyLocation() WRONG, not easiest, returns null where the Gen Utility func prevails!
        if((mCurrentLocation = GeneralUtility.getCurrentLocation(mContext)) != null) {
            LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            // Zoom , Milliseconds , callback
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));

            // Returning true will tell the map not to do the default zoom on click
            return true;
        }
        // And of course, returning false will do default zoom on click. yay @ Goggle .
        return false;
    }

    /** Send a request to the GoogleMap Routing api, display a route to the final destination
     *  - Try to say final destination as many times as possible - truck yeah -
     *  */
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mMap == null)
            return true;

        if (isOnShipment)
            return true;
        // Make sure the user cannot do this logic if they click the end marker
        // really obnoxious that you can't set them not-clickable
        // bad google
        if (marker.equals(endMarker) || marker.equals(selectedMarker))
            return true;

        // Delete the current line if there is one already drawn
        if (mDirectionParser != null)
            mDirectionParser.stopAndRemove();


        String selectedObjId = shipmenObjIdList.get(markerList.indexOf(marker));

        Cursor data = mContext.getContentResolver().query(
                PalletDbContract.UnacceptedShipmentEntry.buildShipmentWithObjIdUri(selectedObjId),
                sFullProjection,
                null,
                null,
                null
        );

        while (data.moveToNext()) {
            mSelectedShipment = new Shipment();
            try {
                mSelectedShipment.setupFromFullProjection(data);
            } catch (GeneralUtility.DateException e) {
                Log.e(LOG_TAG, "Couldn't parse dates in unaccepted shipment: " + mSelectedShipment.getId());
            }
        }
        // hide all markers and just put a pin at the end location
        markersVisible = false;
        for (Marker m: markerList) {
            m.setVisible(false);
        }

        // More efficient than checking in every iteration
        marker.setVisible(true);
        marker.showInfoWindow();

        // Add the marker
        endMarker = mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_end_flag_checkered))
                                        .position(mSelectedShipment.getDropoffLatLng()));

        // Draw the route from marker to endpoint! Asynchronous !
        drawRoute(marker.getPosition(), mSelectedShipment.getDropoffLatLng());

        // Zoom to a nice viewing area
        // Todo: Is it more costly to keep referencing through functions or storing in a variable?
        // # basic prog skills still working on #
        LatLng[] shipmentPoints = {marker.getPosition(), mSelectedShipment.getDropoffLatLng()};
        animateAround(shipmentPoints);
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (mMap == null)
            return;

        // Don't be letting no one mess with the unaccepted or nothin
        // when they're on a shipment
        if (isOnShipment) {
            animateAround(((GlobalApp) mContainerActivity.getApplication()).getCurrentShipment());
            return;
        } else {
            // Remove the end marker if the dood/doodette isn't on a shipment
            if (endMarker != null)
                endMarker.remove();
        }

        // Zoom back to the current location
        if(mCurrentLocation != null) {
            LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            // Zoom , Milliseconds , callback
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 9));
        }

        // Toggle marker click
        if (!markersVisible) {
            for (Marker marker: markerList) {
                marker.setVisible(true);
            }
        }

        // Remove start and end marker if they are there!
        if (endMarker != null)
            endMarker.remove();

        // Remove the trip route
        if (mDirectionParser != null)
            mDirectionParser.stopAndRemove();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        // Launch the offer activity intent cuz itz a dialog
        Intent offerIntent = new Intent(mContainerActivity, OfferShipmentActivity.class);
        offerIntent.putExtra(ServerUtility.SHIPMENT_KEY, mSelectedShipment);
        mContext.startActivity(offerIntent);
    }


    /* SECTION */
    /* CURSOR LOADERS */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Doesn't matter what order, why waste processing on ordering?
        //String sortOrder = PalletDbContract.UnacceptedShipmentEntry.COLUMN_PRICE + " ASC";
        Uri unacceptedUri = PalletDbContract.UnacceptedShipmentEntry.buildUnacceptedShipmentsUri();

        return new CursorLoader(mContext,
                unacceptedUri,
                sObjIdProjectionWithLatLng,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (isOnShipment)
            return;

        while (data.moveToNext()) {
            // Just add the obj id to the list!
            // Then we don't have to store a shit-load-of-shitments !
            // Query on a per base instance!
            String id = data.getString(1);

            // Only add another marker to the map if it is not already there!
            if (!shipmenObjIdList.contains(id)) {
                shipmenObjIdList.add(id);

                LatLng shipLatLng = new LatLng(
                        data.getDouble(2),
                        data.getDouble(3)
                );

                Marker newMarker = mMap.addMarker(new MarkerOptions().title("placeholder").snippet("placeholder")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.pallet_shipment_icon_small))
                        .position(shipLatLng));

                markerList.add(newMarker);
            }
        }
        mUnacceptedCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mUnacceptedCursor = null;
    }


    /* SECTION */
    private void drawRoute(LatLng start, LatLng end) {
        // Builds a request for the route from the clicked marker's latlng to the destination
        String requestUrl = ServerUtility.getGoogleDirectionsUriString(start, end);
        // Send request
        // Parse the data in a background thread
        ServerRequestTask directionsRequest = new ServerRequestTask(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(JSONObject result) {
                mDirectionParser.parseInBackground(result);
                // mMap.setTrafficEnabled(true);
                // Hopefully this takes some of the load off

                // Show the route traffic upon downloading .. jk
                // For now traffic is too much to deal with
                // Maybe on a tablet
            }
        });
        directionsRequest.execute(requestUrl, "GET");
    }

    /* SECTION */
    /* PRIVATE CLASSES */
    private class ShipmentInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final String LOG_TAG = ShipmentInfoWindowAdapter.class.getSimpleName();

        private final View infoContentsView;

        public ShipmentInfoWindowAdapter(LayoutInflater inflater) {
            infoContentsView = inflater.inflate(R.layout.info_window_unaccepted, null); // wutever
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return infoContentsView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }
}
