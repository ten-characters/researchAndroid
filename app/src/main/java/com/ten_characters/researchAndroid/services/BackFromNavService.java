package com.ten_characters.researchAndroid.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.activities.MainActivity;

/**
 * Created by austin on 8/07/15.
 */
public class BackFromNavService extends Service {

    private static final String LOG_TAG = BackFromNavService.class.getSimpleName();

    private WindowManager mWindowManager;
    private ImageButton backButton;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupButtons();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE
        );

        params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;

        // Set the button's height to 10% of the
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        params.width = (int) (size.y * .1);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // This is the x (and y) - offset. 0 so that it will hug the side of the screen
        params.x = 0;
        params.y = -75;

        mWindowManager.addView(backButton, params);

    }

    private void setupButtons() {
        backButton = new ImageButton(this);

        backButton.setImageResource(R.drawable.pallet_nameless_logo_small);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch an intent back to the Pallet main activity!
                Intent palletIntent = new Intent(getApplicationContext(), MainActivity.class);
                palletIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                palletIntent.putExtra(getResources().getString(R.string.nav_back_key), true);
                startActivity(palletIntent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (backButton == null) {
            setupButtons();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backButton != null) {
            mWindowManager.removeView(backButton);
        }
    }
}
