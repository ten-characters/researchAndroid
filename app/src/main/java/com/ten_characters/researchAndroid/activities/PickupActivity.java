package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import net.doo.snap.ScanbotSDK;
import net.doo.snap.camera.ContourDetectorFrameHandler;
import net.doo.snap.camera.PictureCallback;
import net.doo.snap.camera.ScanbotCameraView;
import net.doo.snap.entity.Page;
import net.doo.snap.entity.SnappingDraft;
import net.doo.snap.lib.detector.ContourDetector;
import net.doo.snap.lib.detector.DetectionResult;
import net.doo.snap.persistence.PageFactory;
import net.doo.snap.persistence.cleanup.Cleaner;
import net.doo.snap.process.DocumentProcessingResult;
import net.doo.snap.process.DocumentProcessor;
import net.doo.snap.process.draft.DocumentDraftExtractor;
import net.doo.snap.process.util.DocumentDraft;
import net.doo.snap.ui.PolygonView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class PickupActivity extends ActionBarActivity implements PictureCallback, OnTaskCompleted {

    private static final String LOG_TAG = PickupActivity.class.getSimpleName();

    private Button mCaptureButton;

    private static File mTempImage;
    private static File mOutputFile;

    private Shipment mShipment;

    // SCANNING STUFFZ
    private ScanbotSDK mScanbotSDK;
    private PageFactory mPageFactory;
    private DocumentDraftExtractor mDocExtractor;
    private DocumentProcessor mDocProcessor;
    private Cleaner mDocCleaner;
    private ScanbotCameraView mScannerView;

    // For Actual image overlays on scannerview
    private ContourDetectorFrameHandler mContourHandler;
    private PolygonView mPolygonView;
    private List<PointF> mPolygon;

    private static final int REVIEW_SCAN_REQUEST = 1;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup);

        // Snag the shipment from the intent
        mShipment = (Shipment) getIntent().getSerializableExtra(GeneralUtility.SHIPMENT_INTENT_KEY);

        // Get the button so we can easily change the text
        mCaptureButton = (Button) findViewById(R.id.take_photo_button);

        mScannerView = (ScanbotCameraView) findViewById(R.id.scan_view);
        mScannerView.useFlash(true);
        mScannerView.addPictureCallback(this);

        // Setup an instance of the Scanbot SDK and get the features we will use
        mScanbotSDK = new ScanbotSDK(this);
        mPageFactory = mScanbotSDK.pageFactory();
        mDocExtractor = mScanbotSDK.documentDraftExtractor();
        mDocProcessor = mScanbotSDK.documentProcessor();
        mDocCleaner = mScanbotSDK.cleaner();


        // Add the contour detection for the polygon overlay
        mContourHandler = ContourDetectorFrameHandler.attach(mScannerView);
        mContourHandler.addResultHandler(new ContourDetectorFrameHandler.ResultHandler() {
            @Override
            public boolean handleResult(ContourDetectorFrameHandler.DetectedFrame detectedFrame) {
                //Log.v(LOG_TAG, detectedFrame.detectionResult.name());
                return false;
            }
        });
        mPolygonView = (PolygonView) findViewById(R.id.polygonView);
        mContourHandler.addResultHandler(mPolygonView);

        // Setup the filez
        mTempImage = new File(getFilesDir(), "tempScan.jpg");
        mOutputFile = new File(getFilesDir(), "scanned.pdf");

        // Launch an instruction dialog
        // Alert the driver to  sign the document and then upload it!
        final PickupInstructionsDialogFragment alertFrag = new PickupInstructionsDialogFragment();
        alertFrag.show(getFragmentManager(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When we don't need the camera must release it and thus the picture captured
        // Set everything back to defaults
        mScannerView.onPause();
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.take_photo_button:
                mScannerView.takePicture(true);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REVIEW_SCAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Make sure to see if there is a new polygon sent back
                mPolygon = data.getParcelableArrayListExtra("polygon");
                // Make a request to scan the document with the given polygon, and then submit it afterwards
                scanDoc(mPolygon, true);
            } else {
                retake();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendPickupRequest() {
        final PalletServer uploadServer = new PalletServer(this, this);
        // Send the photo to the server
        // Creates a progress dialog to be updated by the PalletServer
        ProgressDialog uploadProgDialog = new ProgressDialog(this);
        uploadProgDialog.setTitle("Hold tight ...");
        uploadProgDialog.setMessage("Uploading the Proof of Delivery!");
        uploadProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        uploadServer.setProgressDialog(uploadProgDialog);
        uploadServer.pickupShipment(Uri.fromFile(mOutputFile), mShipment);
    }


    @Override
    public void onPictureTaken(byte[] imageBytes, int i) {
        // First we want to save the picture to a temp file
        // Then we want to launch an activity to confirm the scanned
        // region or change it
        try {
            BufferedOutputStream outBuf = new BufferedOutputStream(new FileOutputStream(mTempImage));
            outBuf.write(imageBytes);
            outBuf.flush();
            outBuf.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't output to the image file!", e);
        }

        // Launch an activity where they can review the photo taken, either accepting
        // to submit or rejecting to take another
        // Listen for the result of the activity to determine which
        Intent reviewScanIntent = new Intent(this, ReviewScanActivity.class);
        reviewScanIntent.putExtra("photopath", mOutputFile.getAbsolutePath());
        startActivityForResult(reviewScanIntent, REVIEW_SCAN_REQUEST);
    }

    /** Should be called when the upload task is completed and we can move on to the next step! */
    @Override
    public void onTaskCompleted(JSONObject result) {
        if (result != null) {
            try {
                Toast.makeText(getApplicationContext(), "Successfully uploaded!", Toast.LENGTH_SHORT).show();
                // Store the new paths in the user
                String new_path = result.getString(ServerUtility.FILE_PATH_KEY);
                // Will have to change to shipment specific
                ((GlobalApp) getApplication()).getCurrentShipment().setBolPath(new_path);
                ((GlobalApp) getApplication()).setIsPickedUp(true);
                // Return to where this was called aka the main screen!
                setResult(Activity.RESULT_OK);
                finish();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing JSON!", e);
            }
        }
    }

    public void scanDoc(@Nullable List<PointF> newBoundsPoly, final boolean sendDocument) {
        // Read the saved image from file into bytes
        byte[] imageBytes = new byte[(int) mTempImage.length()];
        try {
            BufferedInputStream inBuf = new BufferedInputStream(
                    new FileInputStream(mTempImage)
            );
            inBuf.read(imageBytes, 0, imageBytes.length);
            inBuf.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't load temp image from file!", e);
        }

        ScanWorkerTask scanTask = new ScanWorkerTask(mPageFactory, mDocProcessor, mDocExtractor, mDocCleaner, mOutputFile, newBoundsPoly, new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(JSONObject result) {
                if (sendDocument) sendPickupRequest();
                // Otherwise you can do whatever you want with the scan, who r we 2 judge
            }
        });
        scanTask.execute(imageBytes);

        /* Another way to get the file, must wait for the scan to finish
        File test;
        try {
            test = scanTask.get();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Problem getting photo!", e);
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "Problem getting photo!", e);
        }*/

        Log.d(LOG_TAG, "Scanned the image!");
    }

    public void retake() {
        mPolygonView.destroyDrawingCache();
        mScannerView.startPreview();
    }

    private static class ScanWorkerTask extends AsyncTask<byte[], Float, File> {

        private PageFactory mPageFactory;
        private DocumentProcessor mDocProcessor;
        private DocumentDraftExtractor mDocExtractor;
        private Cleaner mDocCleaner;

        private File mOutputFile;
        private List<PointF> mPolygon;

        private OnTaskCompleted mOnFinishedCallback;

        public ScanWorkerTask(PageFactory pageFactory, DocumentProcessor docProcessor,  DocumentDraftExtractor docExtractor,
                              Cleaner docCleaner, File outputFile, @Nullable List<PointF> polygon, @Nullable OnTaskCompleted callbackListener) {
            mPageFactory = pageFactory;
            mDocExtractor = docExtractor;
            mDocProcessor = docProcessor;
            mDocCleaner = docCleaner;
            mOutputFile = outputFile;
            mPolygon = polygon;
            mOnFinishedCallback = callbackListener;
        }

        /** */
        @Override
        protected File doInBackground(byte[]... params) {
            byte[] imageBytes = params[0];


            // First crop and apply the filter
            ContourDetector detector = new ContourDetector();
            // First Process the result

            // Can either provide a polygon or we will detect one the best the sdk can
            if (mPolygon == null) {
                DetectionResult result = detector.detect(imageBytes);
                mPolygon = detector.getPolygonF();
            }

            try {
                // Do the real scanning off of the UI thread and send callbacks!
                // Use the b&w filter (good for documents!)
                // Can play around with the binarized one too
                Bitmap scannedResult = detector.processImageF(imageBytes, mPolygon, ContourDetector.IMAGE_FILTER_BINARIZED);

                publishProgress(0.25f);

                // Create the page from the scanned image
                // This is the next step in processing to output file
                Page pageDoc = mPageFactory.buildPage(scannedResult, scannedResult.getWidth(), scannedResult.getHeight()).page;

                // Now we are done with the bitmap
                scannedResult.recycle();
                publishProgress(0.5f);
                // Now Extract some documents (ok just the first one)
                DocumentDraft docDoc = mDocExtractor.extract(new SnappingDraft(pageDoc))[0];

                // And finally we process the document
                // Should be done in an intent service so that processing doesn't stop even if screen closed
                DocumentProcessingResult resultDoc = mDocProcessor.processDocument(docDoc);

                // Now we get the files!
                publishProgress(0.75f);
                org.apache.commons.io.FileUtils.copyFile(resultDoc.getDocumentFile(), mOutputFile.getAbsoluteFile());
                Log.v(LOG_TAG, "FileDoc path: " + mOutputFile.getAbsolutePath());
                Log.v(LOG_TAG, "FileScreen path: " + mOutputFile.getAbsolutePath());
                Log.v(LOG_TAG, "Finished the scan!");
                // Now clean up all the files
                mDocCleaner.cleanUp();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem creating pages!", e);
            } catch (Exception e) {
                Log.wtf(LOG_TAG, "What in lor's name is this error?", e);
            }

            return mOutputFile;
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(File file) {
            // Make sure file is all good and ready to go!
            // Raise error otherwise!
            super.onPostExecute(file);
        }
    }

    public static class PickupInstructionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(R.string.pickup_instructions_title);
            alertBuilder.setMessage(R.string.pickup_instructions);
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
