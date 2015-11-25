package com.ten_characters.researchAndroid.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ten_characters.researchAndroid.R;

import net.doo.snap.lib.detector.ContourDetector;
import net.doo.snap.lib.detector.DetectionResult;
import net.doo.snap.lib.detector.Line2D;
import net.doo.snap.ui.EditPolygonImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ReviewScanActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String LOG_TAG = ReviewScanActivity.class.getSimpleName();

    private static final String POLYGON = "polygon";
    private EditPolygonImageView mEditPolygonImage;

    private List<PointF> mPolygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_scan);

        getSupportActionBar().hide();

        (findViewById(R.id.submit)).setOnClickListener(this);
        (findViewById(R.id.retake)).setOnClickListener(this);

        // Set the photo
        mEditPolygonImage = (EditPolygonImageView) findViewById(R.id.scanned_imageview);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeFile(getIntent().getStringExtra("photopath"), options);
        mEditPolygonImage.setImageBitmap(bm);

        setPolygon(savedInstanceState);

        new DetectLines().executeOnExecutor(Executors.newSingleThreadExecutor(), bm);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit:
                // Attach the new polygon in an intent to send back to the capture activity !
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra(POLYGON, (ArrayList<PointF>) mEditPolygonImage.getPolygon());
                setResult(RESULT_OK, resultIntent);
                finish();
                break;
            case R.id.retake:
                setResult(RESULT_CANCELED);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(POLYGON, (ArrayList<PointF>) mEditPolygonImage.getPolygon());
    }

    private void setPolygon(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mEditPolygonImage.setPolygon(EditPolygonImageView.DEFAULT_POLYGON);
            return;
        }

        ArrayList<PointF> polygon = savedInstanceState.getParcelableArrayList(POLYGON);
        mEditPolygonImage.setPolygon(polygon);
    }

    /**
     * Detects horizontal and vertical lines of the polygon
     */
    class DetectLines extends AsyncTask<Bitmap, Void, Pair<List<Line2D>, List<Line2D>>> {

        @Override
        protected Pair<List<Line2D>, List<Line2D>> doInBackground(Bitmap... params) {
            Bitmap image = params[0];
            ContourDetector detector = new ContourDetector();
            final DetectionResult detectionResult = detector.detect(image);
            switch (detectionResult) {
                case OK:
                case OK_BUT_BAD_ANGLES:
                case OK_BUT_TOO_SMALL:
                case OK_BUT_BAD_ASPECT_RATIO:
                    return new Pair<>(detector.getHorizontalLines(), detector.getVerticalLines());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Pair<List<Line2D>, List<Line2D>> listListPair) {
            super.onPostExecute(listListPair);
            if (listListPair != null) {
                mEditPolygonImage.setLines(listListPair.first, listListPair.second);
            }
        }
    }
}
