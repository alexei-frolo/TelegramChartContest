package com.froloapp.telegramchart.widget.chartview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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

    private int overlayColor = Color.parseColor("#1791bbf2");
    private int frameColor = Color.parseColor("#39346eba");
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float startStampRel = 0f;
    private float endStampRel = 0f;

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
    }

    private void log(String msg) {
        Log.d("ChartSlider", msg);
    }

    public static interface OnScrollListener {
        void onScroll(ChartSlider slider);
    }

    public void setStamps(float startStampRel, float endStampRel) {
        this.startStampRel = startStampRel;
        this.endStampRel = endStampRel;
        invalidate();
    }

    private float checkRel(float value) {
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
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * startStampRel;
        return x > startStampPos - 5 && x < startStampPos + 5;
    }

    private boolean isFrameRightBorderTouched(float x) {
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * endStampRel;
        return x > endStampPos - 5 && x < endStampPos + 5;
    }

    private boolean isFrameTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * startStampRel;
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * endStampRel;
        return x > startStampPos + 5 && x < endStampPos - 5;
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
        canvas.drawRect(0, 0, width * startStampRel, height, paint);
        canvas.drawRect(width * endStampRel, 0, width, height, paint);

        paint.setColor(frameColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(width * startStampRel, 0, width * endStampRel, height, paint);
    }

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
                    startStampRel = checkRel(startStampRel + frameScrollRel);
                    xDragPos = x;
                    log("Left border dragged");
                    invalidate();
                    return true;
                } else if (scrollState == SCROLL_STATE_RIGHT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    endStampRel = checkRel(endStampRel + frameScrollRel);
                    xDragPos = x;
                    log("Right border dragged");
                    invalidate();
                    return true;
                } else if (scrollState == SCROLL_STATE_FRAME_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    if (frameScrollRel > 0) {
                        frameScrollRel = Math.min(1 - endStampRel, frameScrollRel);
                    } else {
                        frameScrollRel = -Math.min(startStampRel, -frameScrollRel);
                    }
                    startStampRel = checkRel(startStampRel + frameScrollRel);
                    endStampRel = checkRel(endStampRel + frameScrollRel);
                    xDragPos = x;
                    log("Frame dragged");
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
