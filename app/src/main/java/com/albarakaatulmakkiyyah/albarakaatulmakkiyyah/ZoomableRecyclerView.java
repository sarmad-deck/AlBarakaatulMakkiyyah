package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A vertical RecyclerView that supports pinch-to-zoom and double-tap zoom.
 * Vertical scrolling stays with the RecyclerView; horizontal panning and the
 * zoom offset are applied as a canvas transform.
 */
public class ZoomableRecyclerView extends RecyclerView {

    public interface OnZoomChangedListener {
        void onZoomSettled(float scale);
    }

    private static final float MAX_SCALE = 4f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float scale = 1f;
    private float translateX;
    private float translateY;
    private boolean suppressChildTouch;
    @Nullable
    private OnZoomChangedListener zoomListener;

    public ZoomableRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ZoomableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setOnZoomChangedListener(@Nullable OnZoomChangedListener listener) {
        zoomListener = listener;
    }

    public float getScale() {
        return scale;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            suppressChildTouch = false;
        }
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        if (suppressChildTouch) {
            return true;
        }
        MotionEvent transformed = MotionEvent.obtain(ev);
        transformed.offsetLocation(-translateX, -translateY);
        transformed.setLocation(transformed.getX() / scale, transformed.getY() / scale);
        boolean handled = super.dispatchTouchEvent(transformed);
        transformed.recycle();
        return handled;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scale, scale);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private void zoomTo(float newScale, float focusX, float focusY) {
        newScale = Math.max(1f, Math.min(newScale, MAX_SCALE));
        float ratio = newScale / scale;
        translateX = focusX - (focusX - translateX) * ratio;
        translateY = focusY - (focusY - translateY) * ratio;
        scale = newScale;
        clampTranslation();
        invalidate();
    }

    private void clampTranslation() {
        float minX = getWidth() - getWidth() * scale;
        float minY = getHeight() - getHeight() * scale;
        translateX = Math.max(minX, Math.min(translateX, 0f));
        translateY = Math.max(minY, Math.min(translateY, 0f));
    }

    private void cancelChildTouch() {
        if (suppressChildTouch) {
            return;
        }
        suppressChildTouch = true;
        MotionEvent cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
    }

    private void notifyZoomSettled() {
        if (zoomListener != null) {
            zoomListener.onZoomSettled(scale);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            cancelChildTouch();
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            zoomTo(scale * detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            notifyZoomSettled();
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            cancelChildTouch();
            zoomTo(scale > 1f ? 1f : DOUBLE_TAP_SCALE, e.getX(), e.getY());
            notifyZoomSettled();
            return true;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                                float distanceX, float distanceY) {
            if (scale <= 1f || scaleDetector.isInProgress()) {
                return false;
            }
            translateX -= distanceX;
            // At the ends of the list the RecyclerView can't scroll, so reveal
            // the part of the first/last page hidden by the zoom offset instead.
            boolean atTop = !canScrollVertically(-1) && distanceY < 0;
            boolean atBottom = !canScrollVertically(1) && distanceY > 0;
            if (atTop || atBottom) {
                translateY -= distanceY;
            }
            clampTranslation();
            invalidate();
            return true;
        }
    }
}
