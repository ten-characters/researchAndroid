package com.ten_characters.researchAndroid.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.IntroBannerFragment;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.PalletServer;

import me.relex.circleindicator.CircleIndicator;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.ten_characters.researchAndroid.server.ServerUtility.BASE_WEB_URL;
import static com.ten_characters.researchAndroid.server.ServerUtility.DRIVER_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.REGISTER_EXT;

public class IntroBannerActivity extends FragmentActivity {

    private static final String LOG_TAG = IntroBannerActivity.class.getSimpleName();
    private final String ACTIVITY_NAME = "IntroBanner";

    private static final int NUM_PAGES = 8;
    private ViewPager mViewPager;

    private static Tracker mTracker;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_banner);

        mTracker = ((GlobalApp) getApplication()).getDefaultTracker();

        // Set up the view Pager. This is what we're here for!
        mViewPager = (ViewPager) findViewById(R.id.intro_pager);
        mViewPager.setAdapter(
                new IntroBannerPagerAdapter(
                        getSupportFragmentManager()
                )
        );

        CircleIndicator circleIndicator = (CircleIndicator) findViewById(R.id.intro_pager_indicator);
        circleIndicator.setViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Could slowly fade if we wanted to get fancy fancy
            }

            @Override
            public void onPageSelected(int position) {
                mTracker.setScreenName(ACTIVITY_NAME + " " + position);
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());

                // Since we are currently doin this silly thing, here is the workaround
                // To get a full screen solid image/color
                switch (position){
                    case 0:
                        // Set the background of the whole container to the trucks!
                        (findViewById(R.id.intro_act_container))
                                .setBackgroundResource(R.drawable.intro_trucks_start);
                        break;
                    case NUM_PAGES - 1:
                        // Set the background to trucks 2! !
                        (findViewById(R.id.intro_act_container))
                                .setBackgroundResource(R.drawable.intro_trucks_end);
                        break;
                    default:
                        (findViewById(R.id.intro_act_container)).setBackgroundColor(
                                getResources().getColor(R.color.black)
                        );
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Change the width of the sign up button to fill 60% of all screens
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int buttonWidth = (int) (size.x * .6);

        Button buttonView = (Button) findViewById(R.id.intro_signup_button);
        ViewGroup.LayoutParams params = buttonView.getLayoutParams();
        params.width = buttonWidth;
        buttonView.setLayoutParams(params);

        // Set the button's text
        buttonView.setText(R.string.sign_up);

        // Set the on click listener to bring to the application page
        buttonView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Send out an event hit!
                        if (mTracker != null) {
                            mTracker.send(new HitBuilders.EventBuilder()
                                    .setCategory("Intro")
                                    .setAction("Sign up button")
                                    .build());
                        }
                        // Send them to the application!
                        Uri uri = Uri.parse(BASE_WEB_URL).buildUpon()
                                .appendPath(REGISTER_EXT)
                                .appendPath(DRIVER_EXT)
                                .build();
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                }
        );


        // Now comes the real question: Should we steal their emails?
        String email = "";
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccounts();
        PalletServer stealServer = new PalletServer(this);
        for (Account account: accounts) {
            if (account.type.equals("com.google")) {
                email = account.name;
            }
        }

        TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String phone = tMgr.getLine1Number();

        stealServer.sendDownloadedInfo(email, phone);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    // Sends the view to fullscreen immersive mode
    // Thanks @ Glance
    @Override
    @TargetApi(19)
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View view = getWindow().getDecorView().findViewById(R.id.intro_pager);
        if (hasFocus && view != null) {
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    public static class IntroBannerPagerAdapter extends FragmentPagerAdapter {


        public IntroBannerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return IntroBannerFragment.create(position, NUM_PAGES);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

}
