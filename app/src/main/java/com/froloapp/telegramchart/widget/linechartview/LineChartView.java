package com.froloapp.telegramchart.widget.linechartview;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;


public class LineChartView extends AbsLineChartView {
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
    private boolean wasClickedStamp = false;
    private long clickedStamp;
    private float clickedXPosition;
    private float clickedStampAlpha;
    private Drawable stampInfoBackground;
    private OnStampClickListener onStampClickListener;
    private ValueAnimator clickedStampAnimator;
    private final Interpolator clickedStampInterpolator = new AccelerateDecelerateInterpolator();

    private final static Property<LineChartView, Float> CLICKED_STAMP_ALPHA = new Property<LineChartView, Float>(float.class, "clickedStampAlpha") {
        @Override public Float get(LineChartView object) {
            return object.clickedStampAlpha;
        }
        @Override public void set(LineChartView object, Float value) {
            object.clickedStampAlpha = value;
            object.invalidate();
        }
    };

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        // preparing stamp info background
        this.stampInfoBackground = ContextCompat.getDrawable(getContext(), R.drawable.bg_stamp_info);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ValueAnimator a = clickedStampAnimator;
        if (a != null) a.cancel();
    }

    public interface OnStampClickListener {
        void onStampClick(LineChartView view, long timestamp, float timestampX);
    }

    public void setOnStampClickListener(OnStampClickListener l) {
        this.onStampClickListener = l;
    }

    /* *********************************
     ********** HELPER METHODS *********
     ***********************************/

    @Override
    final boolean drawFooter() {
        return true;
    }

    /* *******************************
     ******** DRAWING METHODS ********
     ****************************** */

    @Override
    protected void onDraw(Canvas canvas) {
        drawXAxis(canvas);
        drawYAxis(canvas);
        drawLines(canvas);
        drawClickedTimestamp(canvas);
    }

    private void drawClickedTimestamp(Canvas canvas) {
        LineChartAdapter adapter = getAdapter();
        if (adapter != null && wasClickedStamp) {
            long xAxis = clickedStamp;
            float xPosition = clickedXPosition;
            float x = getXCoor(xPosition);
            stampInfoPaint.setAlpha(255);
            stampInfoPaint.setStyle(Paint.Style.STROKE);
            stampInfoPaint.setColor(getXAxisColor());
            canvas.drawLine(x, getPaddingTop(), x, getMeasuredHeight() - getPaddingBottom(), stampInfoPaint);

            Line fadedChart = getFadedChart();
            for (int i = 0; i < adapter.getLineCount(); i++) {
                Line chart = adapter.getLineAt(i);
                boolean needToDraw;
                float alpha;
                if (chart == fadedChart) {
                    needToDraw = true;
                    alpha = getFadedChartAlpha();
                } else if (adapter.isLineEnabled(chart)) {
                    needToDraw = true;
                    alpha = 1f;
                } else {
                    needToDraw = false;
                    alpha = 0f;
                }

                if (needToDraw) {
                    // drawing dots
                    stampInfoPaint.setStyle(Paint.Style.FILL);
                    stampInfoPaint.setColor(chart.getColor());
                    stampInfoPaint.setAlpha((int) (alpha * 255));
                    int index = adapter.getTimestampIndex(xAxis);
                    long value = chart.getValueAt(index);
                    float y = getYCoor(value);
                    canvas.drawCircle(x, y, stampInfoBigDotRadius, stampInfoPaint);
                    stampInfoPaint.setColor(getXAxisColor());
                    stampInfoPaint.setAlpha((int) (alpha * 255));
                    canvas.drawCircle(x, y, stampInfoSmallDotRadius, stampInfoPaint);
                }
            }

//            // drawing info window
//            final int y = getPaddingTop();
//            final int xm = 10;
//            final int windowW = 100;
//            final int windowH = 100;
//            Drawable stampInfoBackground = this.stampInfoBackground;
//            if (stampInfoBackground != null) { // this condition must always be met
//                stampInfoBackground.setBounds((int) (x + xm), y, (int) (x + xm + windowW), y + windowH);
//                stampInfoBackground.draw(canvas);
//            }
        }
    }

    /* *********************************
     ********* TOUCH CALLBACKS *********
     **********************************/

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //wasClickedStamp = false;
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
                    //log("Clicked at (" + x + ", " + y + ")");
                    // handle click here
                    handleClick(x, y, event.getRawX(), event.getRawY());
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleClick(float x, float y, float rawX, float rawY) {
        LineChartAdapter adapter = getAdapter();
        if (adapter != null) {
            float xPosition = getXPosition(x);
            //this.clickedXPosition = adapter.getClosestTimestampPosition(xPosition);
            // looking for the closest X axis stamp
            long closestTimestamp = adapter.getClosestTimestamp(xPosition);
            this.clickedStamp = closestTimestamp;
            this.clickedXPosition = adapter.getTimestampRelPosition(closestTimestamp);
            float timestampX = getXCoor(clickedXPosition);
            dispatchClicked(clickedStamp, timestampX);

            // this view should know that a stamp was clicked
            this.wasClickedStamp = true;
            invalidate();
        }
    }

    private void dispatchClicked(long timestamp, float timestampX) {
        OnStampClickListener l = this.onStampClickListener;
        if (l != null) {
            l.onStampClick(this, timestamp, timestampX);
        }
    }

    private void fadeInClickedStamp() {
        ValueAnimator a = clickedStampAnimator;
        if (a != null) a.cancel();
    }

    @Override
    public void setAdapter(LineChartAdapter adapter) {
        wasClickedStamp = false;
        super.setAdapter(adapter);
    }

    public void clearClickedStamp() {
        // apply fade in to clicked timestamp here
        wasClickedStamp = false;
        invalidate();
    }
}
