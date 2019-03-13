package com.froloapp.telegramchart.widget.chartview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.widget.Utils;


/**
 * ChartView represents a set of linear charts drawn by different colors;
 * It has three layers to draw: phantom, background and foreground;
 * In the background layer, y axis bars (vertical lines) and x axis stamps (timestamps) are drawn;
 * In the phantom layer, phantom y bars and phantom x stamps that fade out when ChartView changes its start and end timestamps;
 * In the foreground layer, the charts are drawn;
 * P.S. Rel = relative (e.g. percentage)
 */
public class ChartView extends AbsChartView {
    // static
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final int TOUCH_STAMP_THRESHOLD_IN_DP = 5;
    private static final long ANIM_DURATION = 200L;

    // paint tools
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect stampTextBounds = new Rect(); // here we store bounds for stamp text height
    private float axisStrokeWidth;

    // touch
    private float touchStampThreshold;
    private float lastTouchedDownX = 0f;
    private float lastTouchedDownY = 0f;
    private ChartData clickedChart;
    private long clickedStamp;

    // multiply min and max Y axis value by this coefficient
    private float yValueCoefficient = 1f;

    // Background (Axes)
    private float axisAlpha = 1f;

    // y axes
    private int yAxisBarCount = 5;
    private float yAxisStep = 0f;

    // x axes
    private int xAxisStampCount = 5;
    private float xAxisStep = 0f;

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // touch
        touchStampThreshold = Utils.dpToPx(TOUCH_STAMP_THRESHOLD_IN_DP, context);

        // axis paint
        axisStrokeWidth = Utils.dpToPx(1f, context);
        axisPaint.setStrokeWidth(axisStrokeWidth);
        axisPaint.setStyle(Paint.Style.STROKE);
        // color for y axis bars and x axis stamps
        axisPaint.setColor(Color.GRAY);

        // axis text paint
        float textSize = Utils.spToPx(DEFAULT_TEXT_HEIGHT_IN_SP, context);
        axisTextPaint.setStyle(Paint.Style.FILL);
        axisTextPaint.setStrokeWidth(axisStrokeWidth);
        axisTextPaint.setColor(Color.GRAY);
        axisTextPaint.setTextSize(textSize);
    }

    /* *********************************
     ********** HELPER METHODS *********
     ***********************************/

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartView", msg);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // update important values here
        yAxisStep = ((float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom())) / yAxisBarCount;
        log("View measured");
    }

    /* *******************************
     ******** DRAWING METHODS ********
     ****************************** */

    @Override
    protected void onDraw(Canvas canvas) {
        drawPhantoms(canvas);
        drawBackground(canvas);
        drawForeground(canvas);
    }

    /**
     * Draws phantom y axis bars and phantom x axis stamps;
     * @param canvas canvas
     */
    private void drawPhantoms(Canvas canvas) {
        log("Drawing phantoms");
    }

    /**
     * Draws y axis bars and x axis stamps;
     * @param canvas canvas
     */
    private void drawBackground(Canvas canvas) {
        log("Drawing background layer");
        ChartAdapter adapter = getAdapter();
        if (adapter == null) return;
        //int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int startX = getPaddingLeft();
        int stopX = getMeasuredWidth() - getPaddingRight();
        for (int i = 0; i < yAxisBarCount; i++) {
            float y = getMeasuredHeight() - getPaddingBottom() - i * yAxisStep - (axisStrokeWidth / 2 + 1);
            canvas.drawLine(startX, y, stopX, y, axisPaint);
        }

        // drawing timestamp texts
        long timestamp = adapter.getMinTimestamp();
        float timestampRel = 0f;
        float xCoor = getXCoor(timestampRel);
        String text = String.valueOf(timestamp);
        axisTextPaint.getTextBounds(text, 0, text.length(), stampTextBounds);
        final float yCoor = getMeasuredHeight() - getPaddingTop() - stampTextBounds.height() / 2;
        canvas.drawText(text, xCoor, yCoor, axisTextPaint);
        while (adapter.hasNextTimestamp(timestamp)) {
            timestamp = adapter.getNextTimestamp(timestamp);
            timestampRel = adapter.getNextTimestampPosition(timestampRel);
            xCoor = getXCoor(timestampRel);
            canvas.drawText(String.valueOf(timestamp), xCoor, yCoor, axisTextPaint);
        }
    }

    /**
     * Draws line charts;
     * @param canvas canvas
     */
    private void drawForeground(Canvas canvas) {
        log("Drawing foreground layer");
        drawCharts(canvas);
    }

    /* *********************************
     ********* TOUCH CALLBACKS *********
     **********************************/

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchedDownX = event.getX();
                lastTouchedDownY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float y = event.getY();
                if (Math.abs(lastTouchedDownX - x) > touchStampThreshold
                        || Math.abs(lastTouchedDownY - y) > touchStampThreshold)
                    return false;
                break;
            }
            case MotionEvent.ACTION_UP: {
                float x = event.getX();
                float y = event.getY();
                if (Math.abs(lastTouchedDownX - x) < touchStampThreshold
                    && Math.abs(lastTouchedDownY - y) < touchStampThreshold) {
                    log("Clicked at (" + x + ", " + y + ")");
                    // handle click here
                    handleClick(x, y);
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleClick(float x, float y) {

    }
}
