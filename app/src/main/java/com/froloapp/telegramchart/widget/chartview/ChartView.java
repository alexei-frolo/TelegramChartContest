package com.froloapp.telegramchart.widget.chartview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.froloapp.telegramchart.widget.Utils;


/**
 * ChartView represents a set of linear charts drawn by different colors;
 * It has three layers to draw: phantom, background and foreground;
 * In the background layer, y axis bars (vertical lines) and x axis stamps (timestamps) are drawn;
 * In the phantom layer, phantom y bars and phantom x stamps that fade out when ChartView changes its start and end timestamps;
 * In the foreground layer, the charts are drawn;
 * P.S. Rel = relative (e.g. percentage)
 */
public class ChartView extends View {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final int TIMESTAMP_BAR_HEIGHT_IN_DP = 20;

    // paint tools
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bufferPath = new Path(); // buffer path to avoid allocating to many paths for multiple charts
    private int axisColor = Color.GRAY; // color for y axis bars and x axis stamps
    private float textSize = 15f;
    private int timestampBarHeight;

    // Adapter
    private ChartAdapter adapter;

    private float startTimestampRel;
    private float endTimestampRel;

    private int minValue;
    private int maxValue;

    // Ghost background
//    private float ghostAlpha = 0f;
//
//    private List<String> yGhostAxes = new ArrayList<>();
//    private float yGhostAxisStep = 0f;
//
//    private List<String> xGhostAxes = new ArrayList<>();
//    private float xGhostAxisStep = 0f;

    // Phantom background

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
        // axis paint
        axisPaint.setStrokeWidth(Utils.dpToPx(2f, context));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setColor(axisColor);
        axisPaint.setTextSize(textSize);

        // chart paint
        chartPaint.setStrokeWidth(Utils.dpToPx(2f, context));
    }

    private void log(String msg) {
        Log.d("ChartView", msg);
    }

    /**
     * Helper methods
     */
    private float checkTimestampRel(float timestampRel) {
        if (timestampRel < 0f || timestampRel > 1f)
            throw new IllegalArgumentException("Percentage value cannot be lower than 0 or bigger than 1. The given value is " + timestampRel);
        return timestampRel;
    }

    private int getXCoor(float timestampRel, float startTimestampRel, float endTimestampRel) {
        int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        float xRelative = ((float) (timestampRel - startTimestampRel)) / (endTimestampRel - startTimestampRel);
        return (int) (getPaddingLeft() + xRelative * contentWidth);
    }

    private int getYCoor(int value, int minValue, int maxValue) {
        int contentHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - timestampBarHeight;
        float yRelative = ((float) value) / (maxValue - minValue);
        return (int) (getPaddingLeft() + yRelative * contentHeight);
    }

    public void setAdapter(ChartAdapter adapter) {
        this.adapter = adapter;
        resolveChartState();
        invalidate();
    }

    public float getStartTimestampRel() {
        return startTimestampRel;
    }

    public void setStartTimestampRel(float timestampRel) {
        if (startTimestampRel != timestampRel) {
            this.startTimestampRel = checkTimestampRel(timestampRel);
            if (resolveChartState()) {
                invalidate();
            }
        }
    }

    public void setEndTimestampRel(float timestampRel) {
        if (endTimestampRel != timestampRel) {
            this.endTimestampRel = checkTimestampRel(timestampRel);
            if (resolveChartState()) {
                invalidate();
            }
        }
    }

    public float getEndTimestampRel() {
        return endTimestampRel;
    }

    public long getMinTimestamp() {
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            return adapter.getMinXAxis();
        } else return 0;
    }

    public long getMaxTimestamp() {
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            return adapter.getMaxXAxis();
        } else return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Utils.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Utils.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
        timestampBarHeight = TIMESTAMP_BAR_HEIGHT_IN_DP;
        resolveChartState();
    }

    // return true if it needs to be invalidated, false - otherwise
    private boolean resolveChartState() {
        yAxisStep = ((float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom())) / yAxisBarCount;
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            int minValue = adapter.getMinValue(startTimestampRel, endTimestampRel);
            int maxValue = adapter.getMaxValue(startTimestampRel, endTimestampRel);
            if (minValue != this.minValue || maxValue != this.maxValue) {
                this.maxValue = maxValue;
                this.minValue = minValue;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPhantoms(canvas);
        drawBackground(canvas);
        drawForeground(canvas);
    }

    /* *******************************
     ******** DRAWING METHODS ********
     ****************************** */

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
            float y = i * yAxisStep;
            canvas.drawLine(startX, y, stopX, y, axisPaint);
        }
    }

    /**
     * Draws line charts;
     * @param canvas canvas
     */
    private void drawForeground(Canvas canvas) {
        log("Drawing foreground layer");
        //int minValue = adapter.getMinValue(startTimestampRel, endTimestampRel);
        //int maxValue = adapter.getMaxValue(startTimestampRel, endTimestampRel);
        for (int i = 0; i < adapter.getChartCount(); i++) {
            ChartData data = adapter.getChart(i);
            long timestamp = adapter.getNextAxis(startTimestampRel);
            float timestampRel = adapter.getNextAxisRel(startTimestampRel);
            int value = data.getValue(timestamp);

            bufferPath.reset();
            int xCoor = getXCoor(timestampRel, startTimestampRel, endTimestampRel);
            int yCoor = getYCoor(value, minValue, maxValue);
            bufferPath.moveTo(xCoor, yCoor);

            while (adapter.hasNextAxis(timestamp)) {
                timestamp = adapter.getNextAxis(timestamp);
                timestampRel = adapter.getNextAxisRel(timestampRel);
                value = data.getValue(timestamp);
                xCoor = getXCoor(timestampRel, startTimestampRel, endTimestampRel);
                yCoor= getYCoor(value, minValue, maxValue);
                bufferPath.lineTo(xCoor, yCoor);
            }

            chartPaint.setColor(Color.DKGRAY);
            chartPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(bufferPath, chartPaint);
        }
    }
}
