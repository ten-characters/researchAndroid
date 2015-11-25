package com.ten_characters.researchAndroid;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ten_characters.researchAndroid.activities.AuthActivity;
import com.ten_characters.researchAndroid.server.ServerUtility;

import pl.droidsonroids.gif.GifImageView;

/**
 * Created by austin on 8/20/15.
 */
public class IntroBannerFragment extends Fragment {

    private static final String LOG_TAG = IntroBannerFragment.class.getSimpleName();

    private static final String ARG_PAGE = "page_num";
    private static final String ARG_NUM_PAGES = "num_pages";

    private int mPageNumber, mTotalPages;


    private String[] pageHeaders, pageBodies;
    private int[] pageDrawables;

    public static IntroBannerFragment create(int pageNumber, int totalPages) {
        IntroBannerFragment fragment = new IntroBannerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        args.putInt(ARG_NUM_PAGES, totalPages);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        mTotalPages = getArguments().getInt(ARG_NUM_PAGES);

        pageHeaders = getResources().getStringArray(R.array.intro_headers);
        pageBodies = getResources().getStringArray(R.array.intro_bodies);

        TypedArray typedImages = getResources().obtainTypedArray(R.array.intro_drawables);
        pageDrawables = new int[typedImages.length()];

        for (int i = 0; i < typedImages.length(); i++) {
            pageDrawables[i] = typedImages.getResourceId(i, 0);
        }
        typedImages.recycle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        // For dynamically setting size
        Display display = ((Activity) container.getContext()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Build the actual fragment layout here!
        RelativeLayout containerView = (RelativeLayout) inflater.inflate(R.layout.intro_banner_base, container, false);

        // Setup the text views
        TextView headerTextView = (TextView) containerView.findViewById(R.id.intro_header);
        headerTextView.setText(pageHeaders[mPageNumber]);

        TextView bodyTextView = (TextView) containerView.findViewById(R.id.intro_body);
        bodyTextView.setText(pageBodies[mPageNumber]);
        // We want the body text to always have a 10% margin on either side
        bodyTextView.getLayoutParams().width = (int) (size.x * .8);


            // If we are on the last page, add a button to login (if we are ready)
            if (mPageNumber == mTotalPages - 1) {
                // The detail button will either be a login button in production or a contact us button
                Button detailButton = (Button) inflater.inflate(R.layout.button_transparent_white_bordered, container, false);

                // Setup the button with a width to match the sign up button and centered on the screen!
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) (size.x * .6),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER_HORIZONTAL;


                if (BuildConfig.FLAVOR.equals("full")) {
                    detailButton.setText(R.string.title_activity_login);
                    detailButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent loginIntent = new Intent(getActivity().getApplicationContext(), AuthActivity.class);
                            startActivity(loginIntent);
                        }
                    });
                } else {
                    // Add a contact us button if still in demo mode
                    detailButton.setText(R.string.contact_us);
                    detailButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                    "mailto", ServerUtility.PALLET_EMAIL, null));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "About Pallet");
                            startActivity(Intent.createChooser(emailIntent, "Send email..."));
                        }
                    });
                }
                // Find the text container and add it here!
                ((LinearLayout) containerView.findViewById(R.id.intro_text_container)).addView(detailButton, params);
            }

        // Setup the main image on each page
        if (mPageNumber == 0 || mPageNumber == mTotalPages - 1) {
            // On the first and last pages we want the background to be clear
            // Also, add the pallet logo as a scaled bitmap
            // as a replacement for a gif
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeResource(getResources(), pageDrawables[mPageNumber], options);

            ((GifImageView) containerView.findViewById(R.id.intro_image_view))
                    .setImageBitmap(bm);

            for (int i = 0; i < containerView.getChildCount(); i++) {
                (containerView.getChildAt(i)).setBackgroundColor(
                        getResources().getColor(R.color.transparent)
                );
            }

            containerView.setBackgroundColor(
                    getResources().getColor(R.color.transparent)
            );

            return containerView;
        } else {

            ((GifImageView) containerView.findViewById(R.id.intro_image_view))
                    .setImageResource(pageDrawables[mPageNumber]);
            return containerView;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}

