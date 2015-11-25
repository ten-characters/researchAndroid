package com.ten_characters.researchAndroid.views;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by austin on 1/07/15.
 */
public class SignView extends SurfaceView {

    private Path mDrawPath;

    private Paint mDrawPaint, mCanvasPaint;

    private int mPaintColor = Color.BLACK;
    private Canvas mDrawCanvas;
    private Bitmap mDanvasBitmap;
    private boolean mHasSigned = false;

    public SignView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
        setDrawingCacheEnabled(true);
    }

    private void setupDrawing() {
        mDrawPath = new Path();
        mDrawPaint = new Paint();

        mDrawPaint.setColor(mPaintColor);
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setStrokeWidth(8);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);

        mCanvasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /** Just draws a transparent color over the bitmap and then invalidates whats currently there */
    public void redo() {
        mDrawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mDrawPath.reset();
        destroyDrawingCache();
        invalidate();
        mHasSigned = false;
    }

    /** Saves the current bitmap to a temporary file and then returns the path to the saved file */
    public String getSignedDocFilePath(){
        // Save the file in the internal storage and then return the filepath
        // return getDrawingCache();
        ContextWrapper contextWrapper = new ContextWrapper(getContext());

        // Path to our apps directory
        File directory = contextWrapper.getDir("imageDir", Context.MODE_PRIVATE);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "SIGNATURE_" + timeStamp + "_.png";
        File imagePath = new File(directory, imageFileName);

        try {
            FileOutputStream fos = new FileOutputStream(imagePath);
            // Compress and save at max quality
            getDrawingCache().compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Can't save the image!", Toast.LENGTH_SHORT).show();
        }

        return imagePath.getAbsolutePath();
    }

    public boolean hasSigned() {
        return mHasSigned;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDanvasBitmap == null)
            mDanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mDanvasBitmap = Bitmap.createScaledBitmap(mDanvasBitmap, w, h, true);
        mDrawCanvas = new Canvas(mDanvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mDanvasBitmap, 0, 0, mCanvasPaint);
        canvas.drawPath(mDrawPath, mDrawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHasSigned = true;
        float touchX = event.getX();
        float touchY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // When a user touches the screen
                mDrawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                // When user moves finger on screen
                mDrawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                // When user lifts finger up
                mDrawCanvas.drawPath(mDrawPath, mDrawPaint);
                mDrawPath.reset();
                break;
            default:
                // Don't do nuthin
                return false;
        }
        invalidate(); // causes onDraw!
        return true;
    }
}
