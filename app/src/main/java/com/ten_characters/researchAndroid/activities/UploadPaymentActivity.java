package com.ten_characters.researchAndroid.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.ten_characters.researchAndroid.auth.AccountUtility.saveToken;
import static com.ten_characters.researchAndroid.server.ServerUtility.ERROR_CODE_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.NEW_TOKEN_KEY;


public class UploadPaymentActivity extends ActionBarActivity {

    private static final String LOG_TAG = UploadPaymentActivity.class.getSimpleName();

    private OnTaskCompleted listener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            try {
                if (result.has(NEW_TOKEN_KEY)) {
                    // Todo: make a concrete decision if we like this method or the Global one better
                    saveToken(getApplicationContext(), result.getString(NEW_TOKEN_KEY));
                }

                if (result.has(ERROR_CODE_KEY)) {
                    switch (result.getString(ERROR_CODE_KEY)) {
                        case "400":
                        case "404":
                            // should just fall through to the login intent
                        case "403":
                            // Launch Login Intent
                            Intent loginIntent = new Intent(UploadPaymentActivity.this, AuthActivity.class);
                            startActivity(loginIntent);
                            break;
                        case "500":
                            Toast.makeText(getApplicationContext(), "Server error!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    // Go back to the Main activity! Job well done!
                    Intent mainIntent = new Intent(UploadPaymentActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem getting data from JSON!", e);
            }
        }

    };

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
    }

   public void onButtonClick(View button) {
       CheckBox tosBox = (CheckBox) findViewById(R.id.payment_tos_checkbox);
       if (tosBox.isChecked()) {
           // Send upload off!
           String routing = ((EditText) findViewById(R.id.edit_routing_number)).getText().toString();
           String account = ((EditText) findViewById(R.id.edit_account_number)).getText().toString();
           PalletServer server = new PalletServer(this, listener);
           server.uploadPayment(routing, account);
       } else {
           Toast.makeText(this, "Please read the Terms of Service!", Toast.LENGTH_SHORT).show();
       }
   }

    public void onLinkClick(View linkView) {
        switch (linkView.getId()) {
            case R.id.tos_service_link:
                Uri serviceUri = Uri.parse(getResources().getString(R.string.payment_tos_service_link));
                startActivity(new Intent(Intent.ACTION_VIEW, serviceUri));
                break;
            case R.id.tos_bank_link:
                Uri bankUri = Uri.parse(getResources().getString(R.string.payment_tos_bank_link));
                startActivity(new Intent(Intent.ACTION_VIEW, bankUri));
                break;
        }
    }
}
