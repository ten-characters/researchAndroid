package com.ten_characters.researchAndroid.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by austin on 7/23/15.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOG_TAG = CameraPreview.class.getSimpleName();

    public static final int STATE_PREVIEW = 0;
    public static final int STATE_FROZEN = 1;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Context mContext;
    private int mState;

    // To be able to embed this in difference views
    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // Open the camera for them .. lazy bums
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mCamera = Camera.open(0);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mContext = context;
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    /** Not ready yet ! */
    public void switchCamera() {
        int cameraId = -1;
        int numCams = Camera.getNumberOfCameras();

        if (numCams == 1)
            return;

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        for (int i = 0; i < numCams; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mCamera = Camera.open(cameraId);
    };

    public boolean isFrozen() {
        return mState == STATE_FROZEN;
    }

    public void freezeFrame() {
        if (mCamera != null) {
            mState = STATE_FROZEN;
        }
    }

    public void unFreezeFrame() {
        if (mCamera != null) {
            mCamera.startPreview();
            mState = STATE_PREVIEW;
        }
    }

    public void touchFocus(final Rect focusRect) {
        // Don't fux around if we don't have the camera
        if (mCamera == null)
            return;

        try {
            // Has to be a list but really we want but one
            List<Camera.Area> focusList = new ArrayList<>();
            Camera.Area focusArea = new Camera.Area(focusRect, 1000);
            focusList.add(focusArea);

            // Now set the camera to parameters of focus !
            Camera.Parameters params = mCamera.getParameters();
            // Update the params
            params.setFocusAreas(focusList);
            params.setMeteringAreas(focusList);
            // Set 'em!
            mCamera.setParameters(params);

            // Now try to auto focus on that area!
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success)
                        mCamera.cancelAutoFocus();
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't autofocus!", e);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // Only want touches!
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return super.onTouchEvent(event);

        float x = event.getX(), y = event.getY();

        // Just need a general area of where they touched
        Rect touchRect = new Rect(
                (int) x - 100,
                (int) y - 100,
                (int) x + 100,
                (int) y + 100
        );

        // Todo: go back to this math and see if we can customize it to something not weird
        // Normalize
        final Rect focusRect = new Rect(
                touchRect.left * 2000/getWidth() - 1000,
                touchRect.top * 2000/getHeight() - 1000,
                touchRect.right * 2000/getWidth() - 1000,
                touchRect.bottom * 2000/getHeight() - 1000
        );

        touchFocus(focusRect);

        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.wtf(LOG_TAG, "Surface Created!");
        try {
            setCameraDisplayOrientation(((Activity)mContext), 0, mCamera);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't start camera display!", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.wtf(LOG_TAG, "Surface Changed!");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.wtf(LOG_TAG, "Surface Destroyed!");
        if (mCamera != null)
            mCamera.stopPreview();
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}

