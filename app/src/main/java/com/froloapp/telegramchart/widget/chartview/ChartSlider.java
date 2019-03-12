package com.froloapp.telegramchart.widget.chartview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.widget.Utils;

public class ChartSlider extends View {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 30;
    private static final int DEFAULT_FRAME_STROKE_WIDTH_IN_DP = 3;

    /**
     * The Slider is not currently scrolling.
     */
    public static final int SCROLL_STATE_IDLE = 0;
    /**
     * The Slider is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_FRAME_DRAGGING = 1;
    /**
     * The Slider frame left border is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_LEFT_BORDER_DRAGGING = 2;
    /**
     * The Slider frame right border is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_RIGHT_BORDER_DRAGGING = 3;
    /**
     * The Slider is currently animating to a final position while not under
     * outside control.
     */
    public static final int SCROLL_STATE_SETTLING = 4;

    private int scrollState = SCROLL_STATE_IDLE;
    private float xDragPos = 0f;
    // If a finger touches a border in a place +- this threshold then the border must be under drag
    private float touchBorderThreshold;

    private int overlayColor = Color.parseColor("#1791bbf2");
    private int frameColor = Color.parseColor("#39346eba");
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float startXPosition = 0f;
    private float stopXPosition = 1f;

    // SCROLL LISTENER
    private OnScrollListener listener;

    public ChartSlider(Context context) {
        this(context, null);
    }

    public ChartSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        paint.setStrokeWidth(Utils.dpToPx(DEFAULT_FRAME_STROKE_WIDTH_IN_DP, context));
        touchBorderThreshold = Utils.dpToPx(10f, context);
    }

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartSlider", msg);
        }
    }

    public interface OnScrollListener {
        void onScroll(ChartSlider slider, float startStampRel, float endStampRel);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.listener = listener;
    }

    private void dispatchScrolled(float startXPosition, float stopXPosition) {
        OnScrollListener l = listener;
        if (l != null) {
            l.onScroll(this, startXPosition, stopXPosition);
        }
    }

    public void setPositions(float startXPosition, float stopXPosition) {
        this.startXPosition = startXPosition;
        this.stopXPosition = stopXPosition;
        invalidate();
    }

    private float checkPosition(float value) {
        if (value < 0)
            return 0;
        if (value > 1)
            return 1;
        return value;
    }

    private int getViewContentWith() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    private boolean isFrameLeftBorderTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * startXPosition;
        return x > startStampPos - touchBorderThreshold && x < startStampPos + touchBorderThreshold;
    }

    private boolean isFrameRightBorderTouched(float x) {
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * stopXPosition;
        return x > endStampPos - touchBorderThreshold && x < endStampPos + touchBorderThreshold;
    }

    private boolean isFrameTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * startXPosition;
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * stopXPosition;
        return x > startStampPos + touchBorderThreshold && x < endStampPos - touchBorderThreshold;
    }

    private boolean canCompressFrame(float startXPosition, float stopXPosition) {
        //return true;
        return stopXPosition - startXPosition > 0.2f; // just fifth part. Change it if you need
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Utils.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Utils.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        paint.setColor(overlayColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width * startXPosition, height, paint);
        canvas.drawRect(width * stopXPosition, 0, width, height, paint);

        paint.setColor(frameColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(width * startXPosition, 0, width * stopXPosition, height, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                 // Detect if user starts dragging frame of one of frame borders
                float x = event.getX();
                xDragPos = x;
                if (isFrameLeftBorderTouched(x)) {
                    scrollState = SCROLL_STATE_LEFT_BORDER_DRAGGING;
                    log("Started dragging left border");
                    return true;
                } else if (isFrameRightBorderTouched(x)) {
                    scrollState = SCROLL_STATE_RIGHT_BORDER_DRAGGING;
                    log("Started dragging right border");
                    return true;
                } else if (isFrameTouched(x)) {
                    scrollState = SCROLL_STATE_FRAME_DRAGGING;
                    log("Started dragging frame");
                    return true;
                } else return super.onTouchEvent(event);
            }
            case MotionEvent.ACTION_MOVE: {
                if (scrollState == SCROLL_STATE_LEFT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStartXPosition = checkPosition(startXPosition + frameScrollRel);
                    if (canCompressFrame(newStartXPosition, stopXPosition)) {
                        startXPosition = newStartXPosition;
                        log("Left border dragged");
                        dispatchScrolled(startXPosition, stopXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_RIGHT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStopXPosition = checkPosition(stopXPosition + frameScrollRel);
                    if (canCompressFrame(startXPosition, newStopXPosition)) {
                        stopXPosition = newStopXPosition;
                        log("Right border dragged");
                        dispatchScrolled(startXPosition, stopXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_FRAME_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    if (frameScrollRel > 0) {
                        frameScrollRel = Math.min(1 - stopXPosition, frameScrollRel);
                    } else {
                        frameScrollRel = -Math.min(startXPosition, -frameScrollRel);
                    }
                    startXPosition = checkPosition(startXPosition + frameScrollRel);
                    stopXPosition = checkPosition(stopXPosition + frameScrollRel);
                    xDragPos = x;
                    log("Frame dragged");
                    dispatchScrolled(startXPosition, stopXPosition);
                    invalidate();
                    return true;
                }
                return super.onTouchEvent(event);
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                scrollState = SCROLL_STATE_IDLE;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
