package com.ten_characters.researchAndroid.activities;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.auth.AccountUtility;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.userInfo.User;

import static com.ten_characters.researchAndroid.server.ServerUtility.*;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashActivity extends Activity {

    private static final String LOG_TAG = SplashActivity.class.getSimpleName();
    private PalletServer mServer;
    private boolean serverTimedOut = false; // Todo: account for timeout!

    private OnTaskCompleted listener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            // Wait for a response with the confirmation / new_token
            // Then finish
            if (result != null && !serverTimedOut) {
                try {
                    if (result.has(ERROR_CODE_KEY)) {
                        switch (result.getString(ERROR_CODE_KEY)) {
                            case "400":
                            case "403":
                            case "404":
                                Intent launchLogin = new Intent(SplashActivity.this, AuthActivity.class);
                                startActivity(launchLogin);
                                finish();
                                break;
                            case "500":
                                Toast.makeText(getApplicationContext(), "Server error!", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    } else {
                        ((GlobalApp)getApplication()).setCurrentUser(
                                new User(getApplicationContext(), result.getJSONObject(USER_KEY)));
                        Intent launchMain = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(launchMain);
                        finish();
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Problem getting data from JSON!", e);
                }
            }
        }
    };

    // Todo: account for preloading data from server
    // Check if the user is signed in,
    // if YES --> preload data
    // if NO --> send to login page
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if there is a user signed in!
        // Since we only allow one account to be signed in at a time, we can be sure if
        // the account list is not empty then we have our user

        AccountManager accountManager = AccountManager.get(getBaseContext());
        if (accountManager.getAccountsByType(AccountUtility.ACCOUNT_TYPE).length != 0) {
            setContentView(R.layout.activity_splash);

            // Decode the logo into a bitmap to fit and play nice
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.pallet_logo_large, options);
            ((ImageView) findViewById(R.id.pallet_logo_imageview)).setImageBitmap(bm);

            mServer = new PalletServer(getApplicationContext(), listener);
            mServer.getCurrentUser();

            // Play the coolio sound hehe
            /*SoundPool sp = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);

            int soundId = sp.load(this, R.raw.truck_idle, 1);
            sp.play(soundId, 1, 1, 0, 0, 1);
            MediaPlayer mp = MediaPlayer.create(this, R.raw.truck_idle);
            mp.start();*/
        } else {
            finish();
            // If there is no user, that means they have never signed in, and we can show them
            // the fancy new slide pager !
            Intent introIntent = new Intent(SplashActivity.this, IntroBannerActivity.class);
            startActivity(introIntent);
        }

    }
}

