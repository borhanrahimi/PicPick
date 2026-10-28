package edu.utsa.cs3443.picpick;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.*;
import androidx.appcompat.widget.AppCompatImageView;

/** Fit-center image with bounded pinch zoom, pan, and double-tap reset/zoom. */
public class ZoomImageView extends AppCompatImageView {
    private Matrix transform;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector taps;
    private float scale = 1f, minimumScale = 1f, lastX, lastY;
    private boolean dragging;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        transform = new Matrix(); setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float next = Math.max(1f, Math.min(5f, scale * detector.getScaleFactor()));
                transform.postScale(next / scale, next / scale, detector.getFocusX(), detector.getFocusY());
                scale = next; constrain(); return true;
            }
        });
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(android.view.MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(android.view.MotionEvent e) { return performClick(); }
            @Override public boolean onDoubleTap(android.view.MotionEvent e) {
                if (scale > 1.1f) resetZoom();
                else { transform.postScale(2.5f, 2.5f, e.getX(), e.getY()); scale = 2.5f; constrain(); }
                return true;
            }
        });
    }
    @Override public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (transform != null) resetZoom();
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh); if (transform != null) resetZoom();
    }
    public void resetZoom() {
        Drawable image = getDrawable();
        if (image == null || getWidth() == 0 || getHeight() == 0) return;
        float width = image.getIntrinsicWidth(), height = image.getIntrinsicHeight();
        if (width <= 0 || height <= 0) return;
        minimumScale = Math.min(getWidth() / width, getHeight() / height);
        transform.setScale(minimumScale, minimumScale);
        transform.postTranslate((getWidth() - width * minimumScale) / 2, (getHeight() - height * minimumScale) / 2);
        scale = 1f; setImageMatrix(transform);
    }
    private void constrain() {
        Drawable image = getDrawable(); if (image == null) return;
        RectF bounds = new RectF(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        transform.mapRect(bounds);
        float dx = bounds.width() < getWidth() ? (getWidth() - bounds.width()) / 2 - bounds.left
                : bounds.left > 0 ? -bounds.left : bounds.right < getWidth() ? getWidth() - bounds.right : 0;
        float dy = bounds.height() < getHeight() ? (getHeight() - bounds.height()) / 2 - bounds.top
                : bounds.top > 0 ? -bounds.top : bounds.bottom < getHeight() ? getHeight() - bounds.bottom : 0;
        transform.postTranslate(dx, dy); setImageMatrix(transform);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) return super.onTouchEvent(event);
        scaleDetector.onTouchEvent(event); taps.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX(); lastY = event.getY(); dragging = true; break;
            case MotionEvent.ACTION_POINTER_DOWN: dragging = false; break;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress() && scale > 1f) {
                    transform.postTranslate(event.getX() - lastX, event.getY() - lastY); constrain();
                }
                lastX = event.getX(); lastY = event.getY(); break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: dragging = false; break;
            default: break;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
