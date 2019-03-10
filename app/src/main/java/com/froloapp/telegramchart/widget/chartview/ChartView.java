package com.froloapp.telegramchart.widget.chartview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.froloapp.telegramchart.widget.Utils;

public class ChartView extends View {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final int TIMESTAMP_BAR_HEIGHT_IN_DP = 20;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int timestampBarHeight;

    private final Path bufferPath = new Path();

    // Adapter
    private ChartAdapter adapter;

    private long startTimestamp;
    private long endTimestamp;

    private long minValue;
    private long maxValue;

    private int yAxisBarCount = 5;
    private int xAxisStampCount = 10;

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
        paint.setStrokeWidth(Utils.dpToPx(2f, context));
    }

    /**
     * Helper methods
     */
    private long checkTimestamp(long timestamp, long minTimestamp, long maxTimestamp) {
        if (timestamp < minTimestamp)
            return minTimestamp;
        if (timestamp > maxTimestamp)
            return maxTimestamp;
        return timestamp;
    }

    private int getXCoor(long timestamp, long startTimestamp, long endTimestamp) {
        int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        float xRelative = ((float) timestamp) / (endTimestamp - startTimestamp);
        return (int) (getPaddingLeft() + xRelative * contentWidth);
    }

    private int getYCoor(int value, int minValue, int maxValue) {
        int contentHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - timestampBarHeight;
        float yRelative = ((float) value) / (maxValue - minValue);
        return (int) (getPaddingLeft() + yRelative * contentHeight);
    }

    public void setAdapter(ChartAdapter adapter) {
        this.adapter = adapter;
        //resolveChartState();
        invalidate();
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long timestamp) {
        if (startTimestamp != timestamp) {
            this.startTimestamp = checkTimestamp(timestamp, getMinTimestamp(), getMaxTimestamp());
            if (resolveChartState()) {
                invalidate();
            }
        }
    }

    public void setEndTimestamp(long timestamp) {
        if (endTimestamp != timestamp) {
            this.endTimestamp = checkTimestamp(timestamp, getMinTimestamp(), getMaxTimestamp());
            if (resolveChartState()) {
                invalidate();
            }
        }
    }

    public long getEndTimestamp() {
        return endTimestamp;
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
    }

    // return true if it needs to be invalidated, false - otherwise
    private boolean resolveChartState() {
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            int minValue = adapter.getMinValue(startTimestamp, endTimestamp);
            int maxValue = adapter.getMaxValue(startTimestamp, endTimestamp);
            if (minValue != this.minValue || maxValue != this.maxValue) {
                this.minValue = maxValue;
                this.minValue = minValue;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int minValue = adapter.getMinValue(startTimestamp, endTimestamp);
        int maxValue = adapter.getMaxValue(startTimestamp, endTimestamp);
        for (int i = 0; i < adapter.getChartCount(); i++) {
            ChartData data = adapter.getChart(i);
            long timestamp = startTimestamp;
            int value = data.getValue(timestamp);

            bufferPath.reset();
            int xCoor = getXCoor(timestamp, startTimestamp, endTimestamp);
            int yCoor= getYCoor(value, minValue, maxValue);
            bufferPath.moveTo(xCoor, yCoor);

            while (adapter.hasNextAxis(timestamp)) {
                timestamp = adapter.getNextAxis(timestamp);
                value = data.getValue(timestamp);
                xCoor = getXCoor(timestamp, startTimestamp, endTimestamp);
                yCoor= getYCoor(value, minValue, maxValue);
                bufferPath.lineTo(xCoor, yCoor);
            }

            paint.setColor(Color.DKGRAY);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(bufferPath, paint);
        }
    }
}
