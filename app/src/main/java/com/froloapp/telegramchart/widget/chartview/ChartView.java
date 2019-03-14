package com.froloapp.telegramchart.widget.chartview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;


public class ChartView extends AbsChartView {
    // static
    private static final int DEFAULT_X_AXIS_STAMP_COUNT = 5;
    private static final int DEFAULT_Y_AXIS_BAR_COUNT = 5;

    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final int TOUCH_STAMP_THRESHOLD_IN_DP = 5;
    private static final long ANIM_DURATION = 200L;

    // paint tools
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stampInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect stampTextBounds = new Rect(); // here we store bounds for stamp text height
    private float axisStrokeWidth;
    private int axisColor;
    private float stampInfoBigDotRadius;
    private float stampInfoSmallDotRadius;

    // touch
    private float touchStampThreshold;
    private float lastTouchedDownX = 0f;
    private float lastTouchedDownY = 0f;
    private boolean clickedStamp = false;
    private long clickedXAxis;
    private float clickedXPosition;
    private OnStampClickListener onStampClickListener;

    // multiply min and max Y axis value by this coefficient
    private float yValueCoefficient = 1f;

    // Background (Axes)
    private float axisAlpha = 1f;

    // y axes
    private int yAxisBarCount;
    //private float yAxisStep = 0f;

    // x axes
    private int xAxisStampCount;
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
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChartView, 0, 0);
            xAxisStampCount = typedArray.getColor(R.styleable.ChartView_xAxisStampCount, DEFAULT_X_AXIS_STAMP_COUNT);
            yAxisBarCount = typedArray.getInteger(R.styleable.ChartView_yAxisBarCount, DEFAULT_Y_AXIS_BAR_COUNT);
            axisColor = typedArray.getColor(R.styleable.ChartView_axisColor, Color.GRAY);
            typedArray.recycle();
        } else {
            xAxisStampCount = DEFAULT_X_AXIS_STAMP_COUNT;
            yAxisBarCount = DEFAULT_Y_AXIS_BAR_COUNT;
            axisColor = Color.GRAY;
        }

        // touch
        touchStampThreshold = Utils.dpToPx(TOUCH_STAMP_THRESHOLD_IN_DP, context);

        // axis paint
        axisStrokeWidth = Utils.dpToPx(1f, context);
        axisPaint.setStrokeWidth(axisStrokeWidth);
        axisPaint.setStyle(Paint.Style.STROKE);
        // color for y axis bars and x axis stamps
        axisPaint.setColor(axisColor);

        // axis text paint
        float textSize = Utils.spToPx(DEFAULT_TEXT_HEIGHT_IN_SP, context);
        axisTextPaint.setStyle(Paint.Style.FILL);
        axisTextPaint.setStrokeWidth(axisStrokeWidth);
        axisTextPaint.setColor(axisColor);
        axisTextPaint.setTextSize(textSize);

        // stamp info paint
        stampInfoPaint.setStrokeWidth(axisStrokeWidth);
        stampInfoPaint.setStyle(Paint.Style.STROKE);

        stampInfoBigDotRadius = Utils.dpToPx(4f, context);
        stampInfoSmallDotRadius = Utils.dpToPx(2f, context);
    }

    public interface OnStampClickListener {
        void onStampClick(float x, float y, float rawX, float rawY, long xAxis);
    }

    public void setOnStampClickListener(OnStampClickListener l) {
        this.onStampClickListener = l;
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
        //yAxisStep = ((float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom())) / yAxisBarCount;
        log("View measured");
    }

    /* *******************************
     ******** DRAWING METHODS ********
     ****************************** */

    @Override
    protected void onDraw(Canvas canvas) {
        drawPhantoms(canvas);
        drawYAxisBars(canvas);
        drawForeground(canvas);
        drawClickedTimestamp(canvas);
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

        float yAxisBarStep = (getMaxYValue() - getMinYValue()) / yAxisBarCount * getStretchingY();

        //int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int startX = getPaddingLeft();
        int stopX = getMeasuredWidth() - getPaddingRight();
        for (int i = 0; i < yAxisBarCount; i++) {
            //float y = getMeasuredHeight() - getPaddingBottom() - i * yAxisBarStep - (axisStrokeWidth / 2 + 1);
            float y = getYCoor(getMinYValue() + i * yAxisBarStep) - (axisStrokeWidth / 2 + 1);
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

    private void drawClickedTimestamp(Canvas canvas) {
        ChartAdapter adapter = getAdapter();
        if (adapter != null && clickedStamp) {
            long xAxis = clickedXAxis;
            float xPosition = clickedXPosition;
            float x = getXCoor(xPosition);
            stampInfoPaint.setStyle(Paint.Style.STROKE);
            stampInfoPaint.setColor(axisColor);
            canvas.drawLine(x, getPaddingTop(), x, getMeasuredHeight() - getPaddingBottom(), stampInfoPaint);

            for (int i = 0; i < adapter.getChartCount(); i++) {
                ChartData chart = adapter.getChart(i);
                if (adapter.isVisible(chart)) {
                    stampInfoPaint.setStyle(Paint.Style.FILL);
                    stampInfoPaint.setColor(chart.getColor());
                    long value = chart.getValue(xAxis);
                    float y = getYCoor(value);
                    canvas.drawCircle(x, y, stampInfoBigDotRadius, stampInfoPaint);
                    stampInfoPaint.setColor(Color.WHITE);
                    canvas.drawCircle(x, y, stampInfoSmallDotRadius, stampInfoPaint);
                }
            }
        }
    }

    /* *********************************
     ********* TOUCH CALLBACKS *********
     **********************************/

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //clickedStamp = false;
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
                    handleClick(x, y, event.getRawX(), event.getRawY());
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleClick(float x, float y, float rawX, float rawY) {
        ChartAdapter adapter = getAdapter();
        if (adapter != null) {
            float xPosition = getXPosition(x);
            this.clickedXPosition = adapter.getClosestTimestampPosition(xPosition);
            // looking for the closest X axis stamp
            long closestXAxis = adapter.getClosestTimestamp(xPosition);
            this.clickedXAxis = closestXAxis;
            //dispatchClicked(x, y, rawX, rawY, closestXAxis);

            this.clickedStamp = true;
            invalidate();
        }
    }

    private void dispatchClicked(float x, float y, float rawX, float rawY, long xAxis) {
        OnStampClickListener l = this.onStampClickListener;
        if (l != null) {
            l.onStampClick(x, y, rawX, rawY, xAxis);
        }
    }
}
