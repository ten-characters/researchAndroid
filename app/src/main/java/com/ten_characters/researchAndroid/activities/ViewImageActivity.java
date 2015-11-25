package com.ten_characters.researchAndroid.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnFileTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;

import java.io.File;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/** A very simple activity
 *  Basically all we want to do is download and display an image in an Big ol image view! */

public class ViewImageActivity extends Activity implements OnFileTaskCompleted{

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        // Get the download document filename and send it to the server!
        String fileName = getIntent().getStringExtra(GeneralUtility.FILENAME_INTENT_KEY);

        ProgressDialog downloadProgDialog = new ProgressDialog(this);
        downloadProgDialog.setTitle("Hold tight ...");
        downloadProgDialog.setMessage("Downloading the document!");
        downloadProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        PalletServer server = new PalletServer(this, this);
        server.setProgressDialog(downloadProgDialog);
        server.downloadFile(fileName);
    }

    @Override
    public void onFileTaskCompleted(File result) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap image = BitmapFactory.decodeFile(result.getAbsolutePath(), options);
        ((ImageView) findViewById(R.id.image_view)).setImageBitmap(image);
    }
}
