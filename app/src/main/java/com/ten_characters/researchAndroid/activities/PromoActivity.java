package com.ten_characters.researchAndroid.activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.ServerUtility;

/** Also a very simple activity, just as a quick launch pad */

public class PromoActivity extends ActionBarActivity {
    private static final String LOG_TAG = PromoActivity.class.getSimpleName();
    private final String ACTIVITY_NAME = "Promo";

    private Tracker mTracker;

    private String mPromoCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);

        mPromoCode = getIntent().getStringExtra(GeneralUtility.PROMOCODE_INTENT_KEY);
        ((TextView) findViewById(R.id.promo_code_text)).setText(getString(R.string.promo_code_header) + "\n" + mPromoCode);

        mTracker = ((GlobalApp) getApplication()).getDefaultTracker();
        mTracker.setScreenName(ACTIVITY_NAME);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void onButtonClick(View button) {
        Intent i = null;
        String body = getResources().getString(R.string.promo_body);
        body += "\nPromo code: " + mPromoCode;
        Uri customPromoUri = Uri.parse(ServerUtility.BASE_WEB_URL).buildUpon()
                .appendPath(ServerUtility.REGISTER_EXT)
                .appendPath(ServerUtility.DRIVER_EXT)
                .appendQueryParameter(ServerUtility.REFERRAL_CODE_EXT, mPromoCode)
                .build();
        body += "\n" + customPromoUri.toString();

        final String TRACKER_ACTION;

        // Build the button specific intent
        switch (button.getId()) {
            case R.id.promo_sms_button:
                i = new Intent(Intent.ACTION_VIEW);
                i.setType("vnd.android-dir/mms-sms");
                i.putExtra("sms_body", body);
                TRACKER_ACTION = "text";
                break;
            case R.id.promo_email_button:
                // Using both the SENDTO and mailto: uri scheme to ensure
                // that only mail apps handle this
                i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:"));
                i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.promo_subject));
                i.putExtra(Intent.EXTRA_TEXT, body);
                TRACKER_ACTION = "email";
                break;
            case R.id.promo_share_button:
                i = new Intent(Intent.ACTION_SEND);
                i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.promo_subject));
                i.putExtra(Intent.EXTRA_TEXT, body);
                i.setType("text/plain");
                TRACKER_ACTION = "share";
                break;
            default:
                TRACKER_ACTION = "other";
        }

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(ACTIVITY_NAME)
                .setAction(TRACKER_ACTION)
                .build());

        if (i != null && i.resolveActivity(getPackageManager()) != null)
            startActivity(i);
    }
}
