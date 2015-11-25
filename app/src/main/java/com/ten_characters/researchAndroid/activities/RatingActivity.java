package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RatingActivity extends Activity implements RatingBar.OnRatingBarChangeListener, OnTaskCompleted{

    private static final String LOG_TAG = RatingActivity.class.getSimpleName();

    private PalletServer mServer;
    private RatingBar pickupRatingBar, dropoffRatingBar;
    private Shipment shipment;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);
        mServer = new PalletServer(this, this);
        shipment = (Shipment) getIntent().getSerializableExtra(GeneralUtility.SHIPMENT_INTENT_KEY);

        pickupRatingBar = ((RatingBar)findViewById(R.id.pickup_rating_bar));
        pickupRatingBar.setOnRatingBarChangeListener(this);
        pickupRatingBar.setRating(2.5f);

        dropoffRatingBar = ((RatingBar)findViewById(R.id.dropoff_rating_bar));
        dropoffRatingBar.setOnRatingBarChangeListener(this);
        dropoffRatingBar.setRating(2.5f);
    }

    public void onButtonClick(View button) {
        // The submit button
        mServer.rate(
                pickupRatingBar.getRating(),
                dropoffRatingBar.getRating(),
                shipment
        );
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        // A good ol request
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainIntent.putExtra(GeneralUtility.FINISHED_RATING_INTENT_KEY, true);
        // Don't want it appearing in the backstack!
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        String name = "";
        switch (ratingBar.getId()) {
            case R.id.dropoff_rating_bar:
                name = "Dropoff: ";
                break;
            case R.id.pickup_rating_bar:
                name = "Pickup: ";
                break;
        }

        Toast.makeText(this, name + rating, Toast.LENGTH_SHORT).show();
    }
}
