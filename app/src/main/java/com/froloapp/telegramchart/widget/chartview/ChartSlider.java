package com.froloapp.telegramchart.widget.chartview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;

public class ChartSlider extends AbsChartView {
    // static
    private static final int DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP = 5;
    private static final int DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP = 1;
    private static final float DEFAULT_MAX_FRAME_COMPRESSION = 0.2f;

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

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float frameHorizontalBorderWidth;
    private float frameVerticalBorderWidth;

    private float leftBorderXPosition = 0f;
    private float rightBorderXPosition = 1f;

    private float maxFrameCompression = 0.5f;

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
        int overlayColor;
        int frameBorderColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChartSlider, 0, 0);
            overlayColor = typedArray.getColor(R.styleable.ChartSlider_overlayColor, Color.parseColor("#AAFFFFFF"));
            frameBorderColor = typedArray.getColor(R.styleable.ChartSlider_frameBorderColor, Color.parseColor("#AAC1C1C1"));
            frameHorizontalBorderWidth = typedArray.getDimension(R.styleable.ChartSlider_frameHorizontalBorderWidth,
                    Utils.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context));
            frameVerticalBorderWidth = typedArray.getDimension(R.styleable.ChartSlider_frameVerticalBorderWidth,
                    Utils.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context));
            maxFrameCompression = typedArray.getFloat(R.styleable.ChartSlider_maxFrameCompression, DEFAULT_MAX_FRAME_COMPRESSION);
            typedArray.recycle();
        } else {
            overlayColor = Color.parseColor("#AAFFFFFF");
            frameBorderColor = Color.parseColor("#AAC1C1C1");
            frameHorizontalBorderWidth = Utils.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context);
            frameVerticalBorderWidth = Utils.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context);
            maxFrameCompression = DEFAULT_MAX_FRAME_COMPRESSION;
        }
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(overlayColor);

        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setColor(frameBorderColor);

        touchBorderThreshold = Utils.dpToPx(5f, context);
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

    public void setBorderPositions(float start, float stop) {
        this.leftBorderXPosition = start;
        this.rightBorderXPosition = stop;
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
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * leftBorderXPosition;
        return x > startStampPos - frameHorizontalBorderWidth - touchBorderThreshold
                && x < startStampPos + frameHorizontalBorderWidth + touchBorderThreshold;
    }

    private boolean isFrameRightBorderTouched(float x) {
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * rightBorderXPosition;
        return x > endStampPos - frameHorizontalBorderWidth - touchBorderThreshold
                && x < endStampPos + frameHorizontalBorderWidth + touchBorderThreshold;
    }

    private boolean isFrameTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * leftBorderXPosition;
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * rightBorderXPosition;
        return x > startStampPos + touchBorderThreshold && x < endStampPos - touchBorderThreshold;
    }

    private boolean canCompressFrame(float startXPosition, float stopXPosition) {
        //return true;
        return stopXPosition - startXPosition >= maxFrameCompression; // just fifth part. Change it if you need
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw charts in background
        drawCharts(canvas);

        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        //int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getMeasuredWidth() - getPaddingRight();
        float bottom = getMeasuredHeight() - getPaddingBottom();

        float leftBorder = getPaddingLeft() + width * leftBorderXPosition;
        float rightBorder = getPaddingLeft() + width * rightBorderXPosition;

        // drawing left overlay
        canvas.drawRect(left, top, leftBorder + frameHorizontalBorderWidth, bottom, overlayPaint);
        // drawing right overlay
        canvas.drawRect(rightBorder - frameHorizontalBorderWidth, top, right, bottom, overlayPaint);

        // FRAME
        framePaint.setStrokeWidth(frameHorizontalBorderWidth);
        // Left border
        canvas.drawLine(leftBorder + frameHorizontalBorderWidth / 2, getPaddingTop(),
                leftBorder + frameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), framePaint);
        // Right border
        canvas.drawLine(rightBorder - frameHorizontalBorderWidth / 2, getPaddingTop(),
                rightBorder - frameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), framePaint);

        framePaint.setStrokeWidth(frameVerticalBorderWidth);
        // Top border
        canvas.drawLine(leftBorder, getPaddingTop() + frameVerticalBorderWidth / 2,
                rightBorder, getPaddingTop() + frameVerticalBorderWidth / 2, framePaint);
        // Bottom border
        canvas.drawLine(leftBorder, getMeasuredHeight() - getPaddingBottom() - frameVerticalBorderWidth / 2,
                rightBorder, getMeasuredHeight() - getPaddingBottom() - frameVerticalBorderWidth / 2, framePaint);
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
                    //log("Started dragging left border");
                    return true;
                } else if (isFrameRightBorderTouched(x)) {
                    scrollState = SCROLL_STATE_RIGHT_BORDER_DRAGGING;
                    //log("Started dragging right border");
                    return true;
                } else if (isFrameTouched(x)) {
                    scrollState = SCROLL_STATE_FRAME_DRAGGING;
                    //log("Started dragging frame");
                    return true;
                } else return super.onTouchEvent(event);
            }
            case MotionEvent.ACTION_MOVE: {
                if (scrollState == SCROLL_STATE_LEFT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStartXPosition = checkPosition(leftBorderXPosition + frameScrollRel);
                    if (canCompressFrame(newStartXPosition, rightBorderXPosition)) {
                        leftBorderXPosition = newStartXPosition;
                        //log("Left border dragged");
                        dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_RIGHT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStopXPosition = checkPosition(rightBorderXPosition + frameScrollRel);
                    if (canCompressFrame(leftBorderXPosition, newStopXPosition)) {
                        rightBorderXPosition = newStopXPosition;
                        //log("Right border dragged");
                        dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_FRAME_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    if (frameScrollRel > 0) {
                        frameScrollRel = Math.min(1 - rightBorderXPosition, frameScrollRel);
                    } else {
                        frameScrollRel = -Math.min(leftBorderXPosition, -frameScrollRel);
                    }
                    leftBorderXPosition = checkPosition(leftBorderXPosition + frameScrollRel);
                    rightBorderXPosition = checkPosition(rightBorderXPosition + frameScrollRel);
                    xDragPos = x;
                    //log("Frame dragged");
                    dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
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

    @Override
    public void setAdapter(ChartAdapter adapter) {
        super.setAdapter(adapter);
        setXPositions(0f, 1f);
    }
}
