package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.userInfo.Shipment;
import com.ten_characters.researchAndroid.views.SignView;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SignActivity extends Activity implements View.OnClickListener{
    private static final String LOG_TAG = SignActivity.class.getSimpleName();

    private SignView mSignView;
    private EditText mConsigneeNameView;
    private Shipment shipment;

    private OnTaskCompleted uploadListener = new OnTaskCompleted() {
        @Override
        public void onTaskCompleted(JSONObject result) {
            // Send back to the main screen so they can get more freight!
            Intent mainIntent = new Intent(SignActivity.this, MainActivity.class);
            mainIntent.putExtra(GeneralUtility.FINISHED_SHIPMENT_INTENT_KEY, true);
            mainIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, shipment);
            startActivity(mainIntent);
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

        // Perhaps should move Shipment and User classes to Parcel
        shipment = (Shipment) getIntent().getSerializableExtra(ServerUtility.SHIPMENT_KEY);

        mConsigneeNameView = ((EditText) findViewById(R.id.edit_name_view));

        (findViewById(R.id.signature_redo_button)).setOnClickListener(this);
        (findViewById(R.id.submit_signature_button)).setOnClickListener(this);

        // Alert the driver to have the warehouse sign the phone
        final DropoffInstructionsDialogFragment alertFrag = new DropoffInstructionsDialogFragment();
        alertFrag.show(getFragmentManager(), null);
    }

    @Override
    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.submit_signature_button:
                if (mSignView.hasSigned() && mConsigneeNameView.length() != 0) {
                    // Send the thing to the server!
                    PalletServer uploadServer = new PalletServer(this, uploadListener);

                    ProgressDialog uploadProgDialog = new ProgressDialog(this);
                    uploadProgDialog.setTitle("Hold tight ...");
                    uploadProgDialog.setMessage("Making the Proof of Delivery!");
                    uploadProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    uploadServer.setProgressDialog(uploadProgDialog);
                    uploadProgDialog.dismiss();

                    uploadServer.dropoffShipment(
                            mSignView.getSignedDocFilePath(),
                            mConsigneeNameView.getText().toString(),
                            shipment
                    );
                }
                else {
                    Toast.makeText(this, "Please enter your name and sign!", Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.signature_redo_button:
                mSignView.redo();
                break;
        }
    }

    public static class DropoffInstructionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(R.string.sign_instructions_title);
            alertBuilder.setMessage(R.string.sign_instructions);
            alertBuilder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            return alertBuilder.create();
        }
    }
}
