package com.froloapp.telegramchart.widget.chartview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.widget.Utils;


public class ChartView extends AbsChartView {
    // static
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final int TOUCH_STAMP_THRESHOLD_IN_DP = 5;
    private static final long ANIM_DURATION = 200L;

    // paint tools
    private final Paint stampInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        // stamp info paint
        stampInfoPaint.setStrokeWidth(Utils.dpToPx(1f, context));
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
        //yAxisStep = ((float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom())) / yAxisStampCount;
        log("View measured");
    }

    /* *******************************
     ******** DRAWING METHODS ********
     ****************************** */

    @Override
    protected void onDraw(Canvas canvas) {
        drawXAxis(canvas);
        drawYAxis(canvas);
        drawCharts(canvas);
        drawClickedTimestamp(canvas);
    }

    private void drawClickedTimestamp(Canvas canvas) {
        ChartAdapter adapter = getAdapter();
        if (adapter != null && clickedStamp) {
            long xAxis = clickedXAxis;
            float xPosition = clickedXPosition;
            float x = getXCoor(xPosition);
            stampInfoPaint.setAlpha(255);
            stampInfoPaint.setStyle(Paint.Style.STROKE);
            stampInfoPaint.setColor(getXAxisColor());
            canvas.drawLine(x, getPaddingTop(), x, getMeasuredHeight() - getPaddingBottom(), stampInfoPaint);

            ChartData fadedChart = getFadedChart();
            for (int i = 0; i < adapter.getChartCount(); i++) {
                ChartData chart = adapter.getChart(i);
                boolean needToDraw;
                float alpha;
                if (chart == fadedChart) {
                    needToDraw = true;
                    alpha = getFadedChartAlpha();
                } else if (adapter.isVisible(chart)) {
                    needToDraw = true;
                    alpha = 1f;
                } else {
                    needToDraw = false;
                    alpha = 0f;
                }

                if (needToDraw) {
                    stampInfoPaint.setStyle(Paint.Style.FILL);
                    stampInfoPaint.setColor(chart.getColor());
                    stampInfoPaint.setAlpha((int) (alpha * 255));
                    int index = adapter.getTimestampIndex(xAxis);
                    long value = chart.getValueAt(index);
                    float y = getYCoor(value);
                    canvas.drawCircle(x, y, stampInfoBigDotRadius, stampInfoPaint);
                    stampInfoPaint.setColor(Color.WHITE);
                    stampInfoPaint.setAlpha((int) (alpha * 255));
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

    @Override
    public void setAdapter(ChartAdapter adapter) {
        clickedStamp = false;
        super.setAdapter(adapter);
    }
}
