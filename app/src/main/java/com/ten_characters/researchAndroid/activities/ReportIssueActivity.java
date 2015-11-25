package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.ten_characters.researchAndroid.auth.AccountUtility.saveToken;

/**
 * Created by austin on 8/07/15.
 */
public class ReportIssueActivity extends Activity implements OnTaskCompleted{

    private static final String LOG_TAG = ReportIssueActivity.class.getSimpleName();
    private PalletServer mServer;
    private boolean canDeliver;
    private EditText estimatedDelayEditText;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);

        estimatedDelayEditText = new EditText(this);
        estimatedDelayEditText.setLayoutParams(lp);
        estimatedDelayEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        estimatedDelayEditText.setId(R.id.estimated_delay_input);
        estimatedDelayEditText.setHint(R.string.estimated_delay_prompt);
        estimatedDelayEditText.setTextColor(getResources().getColor(R.color.accent_dark_indigo));
        estimatedDelayEditText.setHintTextColor((getResources().getColor(R.color.accent_dark_indigo)));

        ((Switch) findViewById(R.id.can_deliver_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                canDeliver = ((Switch) findViewById(R.id.can_deliver_switch)).isChecked();
                toggleCanDeliverScreen();
            }
        });
    }

    public void onButtonClick(View button) {
        switch (button.getId()) {
            case R.id.issue_submit_button:
                mServer = new PalletServer(this, this);
                // Pulls the location of the driver from their last updated location
                if (canDeliver) {
                    if(estimatedDelayEditText.getText().toString().equals("")) {
                        Toast.makeText(this, "Please estimate the delay time!", Toast.LENGTH_SHORT).show();
                    } else {
                        mServer.reportIssue(true,
                                Double.parseDouble(estimatedDelayEditText.getText().toString()));
                    }
                } else {
                    mServer.reportIssue(false);
                }

                break;
            case R.id.issue_cancel_button:
                finish();
                break;
        }
    }

    private void toggleCanDeliverScreen() {
        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        if (canDeliver) {
            container.addView(estimatedDelayEditText);
        } else {
            container.removeView(estimatedDelayEditText);
        }
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        try {
            if (result == null) { throw new JSONException("No result"); }
            if (result.has(ServerUtility.NEW_TOKEN_KEY)) {
                saveToken(getApplicationContext(), result.getString(ServerUtility.NEW_TOKEN_KEY));
            }
            if (result.has(ServerUtility.ERROR_CODE_KEY)) {
                switch (result.getString(ServerUtility.ERROR_CODE_KEY)) {
                    case "403":
                        // Stop the updating service as well
                        break;
                    case "500":
                        Toast.makeText(getApplicationContext(), "Server error!", Toast.LENGTH_SHORT).show();
                        break;
                }
            } else {
                Log.d(LOG_TAG, "Successfully submitted Issue!");
                finish();
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem with server response JSON!", e);
        }
    }
}
