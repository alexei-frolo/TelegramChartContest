package com.froloapp.telegramchart.widget.chartview;

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
public class ChartView extends View implements ChartUI {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final int TIMESTAMP_BAR_HEIGHT_IN_DP = 20;
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final int TOUCH_STAMP_THRESHOLD_IN_DP = 10;
    private static final long ANIM_DURATION = 200L;

    // paint tools
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect stampTextBounds = new Rect(); // here we store bounds for stamp text height
    private final Path bufferPath = new Path(); // buffer path to avoid allocating to many paths for multiple charts
    private float axisStrokeWidth;

    // touch
    private float touchStampThreshold;
    private float lastTouchedDownX = 0f;
    private float lastTouchedDownY = 0f;
    private ChartData clickedChart;
    private long clickedStamp;

    // Adapter
    private ChartAdapter adapter;

    // current start and stop positions on X Axis in percentage(from 0 to 1)
    private float startXPercentage;
    private float stopXPercentage;

    // current min and max value on Y axis
    private float minYValue;
    private float maxYValue;
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

    // Animators
    private ValueAnimator minValueAnimator;
    private ValueAnimator maxValueAnimator;

    // Animator update listener
    private final ValueAnimator.AnimatorUpdateListener minYValueUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {
            ChartView.this.minYValue = (float) animation.getAnimatedValue();
            invalidate();
        }
    };
    private final ValueAnimator.AnimatorUpdateListener maxYValueUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {
            ChartView.this.maxYValue = (float) animation.getAnimatedValue();
            invalidate();
        }
    };
    private final Interpolator yValueInterpolator = new AccelerateDecelerateInterpolator();

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

        // chart paint
        chartPaint.setStrokeWidth(Utils.dpToPx(1.5f, context));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // We need to cancel all animations here

        ValueAnimator a1 = minValueAnimator;
        if (a1 != null) a1.cancel();

        ValueAnimator a2 = maxValueAnimator;
        if (a2 != null) a2.cancel();
    }

    /* *********************************
     ********** HELPER METHODS *********
     ***********************************/

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartView", msg);
        }
    }

    private int getXCoor(float p) {
        int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        float xRelative = ((float) (p - startXPercentage)) / (stopXPercentage - startXPercentage);
        return (int) (getPaddingLeft() + xRelative * contentWidth);
    }

    private int getYCoor(float value, float minValue, float maxValue) {
        int contentHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        float yRelative = (value - minValue) / (maxValue - minValue);
        return (int) (getMeasuredHeight() - getPaddingTop() - yRelative * contentHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Utils.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Utils.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
        // update important values here
        yAxisStep = ((float) (measuredHeight - getPaddingTop() - getPaddingBottom())) / yAxisBarCount;
        minYValue = adapter.getMinYValue(startXPercentage, stopXPercentage);
        maxYValue = adapter.getMaxXValue(startXPercentage, stopXPercentage);
        log("View measured");
    }

    /**
     * Checks if min or max value on Y axis has changed;
     * If so then it does phantom magic with current Y axis bars;
     */
    private void checkIfMinOrMaxValueChanged() {
        int minValue = adapter.getMinYValue(startXPercentage, stopXPercentage);
        int maxValue = adapter.getMaxXValue(startXPercentage, stopXPercentage);
        // check min value
        if (minValue != this.minYValue) {
            log("Min Y value changed. Starting animator");
            ValueAnimator oldAnimator = minValueAnimator;
            if (oldAnimator != null) oldAnimator.cancel();

            ValueAnimator newAnimator = ValueAnimator.ofFloat(this.minYValue, minValue);
            newAnimator.addUpdateListener(minYValueUpdateListener);
            newAnimator.setInterpolator(yValueInterpolator);
            newAnimator.setDuration(ANIM_DURATION);
            minValueAnimator = newAnimator;
            newAnimator.start();
        }
        // check max value
        if (maxValue != this.maxYValue) {
            log("Max Y value changed. Starting animator");
            ValueAnimator oldAnimator = maxValueAnimator;
            if (oldAnimator != null) oldAnimator.cancel();

            ValueAnimator newAnimator = ValueAnimator.ofFloat(this.maxYValue, maxValue);
            newAnimator.addUpdateListener(maxYValueUpdateListener);
            newAnimator.setInterpolator(yValueInterpolator);
            newAnimator.setDuration(ANIM_DURATION);
            maxValueAnimator = newAnimator;
            newAnimator.start();
        }
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
        for (int i = 0; i < adapter.getChartCount(); i++) {
            ChartData data = adapter.getChart(i);
//            long timestamp = adapter.getNextTimestamp(startXPercentage);
//            float timestampRel = adapter.getNextTimestampPosition(startXPercentage);
            long timestamp = adapter.getMinTimestamp();
            float timestampRel = 0f;

            int value = data.getValue(timestamp);

            bufferPath.reset();
            int xCoor = getXCoor(timestampRel);
            int yCoor = getYCoor(value, minYValue, maxYValue);
            bufferPath.moveTo(xCoor, yCoor);

            while (adapter.hasNextTimestamp(timestamp)) {
//                timestamp = adapter.getNextTimestamp(timestamp);
//                timestampRel = adapter.getNextTimestampPosition(timestampRel);
                timestamp = adapter.getNextTimestamp(timestamp);
                timestampRel = adapter.getNextTimestampPosition(timestampRel);

                value = data.getValue(timestamp);
                xCoor = getXCoor(timestampRel);
                yCoor = getYCoor(value, minYValue, maxYValue);
                bufferPath.lineTo(xCoor, yCoor);
            }

            chartPaint.setColor(data.getColor());
            chartPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(bufferPath, chartPaint);
        }
    }

    /* *********************************
     ********* PUBLIC INTERFACE ********
     **********************************/

    @Override
    public void setAdapter(ChartAdapter adapter) {
        this.adapter = adapter;
        checkIfMinOrMaxValueChanged();
        invalidate();
    }

    @Override
    public void setStartXPosition(float p) {
        if (startXPercentage != p) {
            this.startXPercentage = p;
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void setStopXPosition(float p) {
        if (stopXPercentage != p) {
            this.stopXPercentage = p;
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void setXPositions(float start, float stop) {
        if (this.startXPercentage != start || this.stopXPercentage != stop) {
            this.startXPercentage = start;
            this.stopXPercentage = stop;
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void show(ChartData chart) {
        // showing a chart here
    }

    @Override
    public void hide(ChartData chart) {
        // hiding a chart here
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
                if (Math.abs(lastTouchedDownX - x) > TOUCH_STAMP_THRESHOLD_IN_DP
                        || Math.abs(lastTouchedDownY - y) > TOUCH_STAMP_THRESHOLD_IN_DP)
                    return false;
                break;
            }
            case MotionEvent.ACTION_UP: {
                float x = event.getX();
                float y = event.getY();
                if (Math.abs(lastTouchedDownX - x) < TOUCH_STAMP_THRESHOLD_IN_DP
                    && Math.abs(lastTouchedDownY - y) < TOUCH_STAMP_THRESHOLD_IN_DP) {
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
