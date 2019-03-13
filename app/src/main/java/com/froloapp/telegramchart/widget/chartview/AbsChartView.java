package com.froloapp.telegramchart.widget.chartview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.BuildConfig;
import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;


/**
 * Helps to draw line charts, handle start and stop x positions and animate appearing or disappearing of a chart;
 */
public class AbsChartView extends View implements ChartUI {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final long ANIM_DURATION = 200L;

    private static final float DEFAULT_CHART_LINE_WIDTH_IN_DP = 1.5f;

    private ChartAdapter adapter;

    // paint tools
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bufferPath = new Path(); // buffer path to avoid allocating to many paths for multiple charts

    // current start and stop positions on X Axis in percentage(from 0 to 1)
    private float startXPercentage;
    private float stopXPercentage;

    // current min and max value on Y axis
    private float minYValue;
    private float maxYValue;

    // Animators
    private ValueAnimator minValueAnimator;
    private ValueAnimator maxValueAnimator;

    // Animator update listener
    private final ValueAnimator.AnimatorUpdateListener minYValueUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {
            AbsChartView.this.minYValue = (float) animation.getAnimatedValue();
            invalidate();
        }
    };
    private final ValueAnimator.AnimatorUpdateListener maxYValueUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {
            AbsChartView.this.maxYValue = (float) animation.getAnimatedValue();
            invalidate();
        }
    };
    private final Interpolator yValueInterpolator = new AccelerateDecelerateInterpolator();

    // Faded in/out chart
    private ChartData fadedChart = null;
    private float fadedChartAlpha = 0f;
    private ObjectAnimator fadedChartAnimator;
    private final Property<AbsChartView, Float> FADED_CHART_ALPHA = new Property<AbsChartView, Float>(float.class, "fadedChartAlpha") {
        @Override public Float get(AbsChartView object) {
            return object.fadedChartAlpha;
        }
        @Override public void set(AbsChartView object, Float value) {
            object.fadedChartAlpha = value;
            invalidate();
        }
    };

    public AbsChartView(Context context) {
        this(context, null);
    }

    public AbsChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float chartLineWidth;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AbsChartView, 0, 0);
            chartLineWidth = typedArray.getDimension(R.styleable.AbsChartView_chartLineWidth, Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context));
            typedArray.recycle();
        } else {
            chartLineWidth = Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context);
        }
        // chart paint
        chartPaint.setStrokeWidth(chartLineWidth);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // We need to cancel all animations here

        ValueAnimator a1 = minValueAnimator;
        if (a1 != null) a1.cancel();

        ValueAnimator a2 = maxValueAnimator;
        if (a2 != null) a2.cancel();

        ValueAnimator a3 = fadedChartAnimator;
        if (a3 != null) a3.cancel();
    }

    /* *********************************
     ********** HELPER METHODS *********
     ***********************************/

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartView", msg);
        }
    }

    /*package-private*/ ChartAdapter getAdapter() {
        return adapter;
    }

    /*package-private*/ int getXCoor(float p) {
        int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        float xRelative = ((float) (p - startXPercentage)) / (stopXPercentage - startXPercentage);
        return (int) (getPaddingLeft() + xRelative * contentWidth);
    }

    /*package-private*/ int getYCoor(float value, float minValue, float maxValue) {
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
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            minYValue = adapter.getMinYValue(startXPercentage, stopXPercentage);
            maxYValue = adapter.getMaxXValue(startXPercentage, stopXPercentage);
        } else {
            minYValue = 0f;
            maxYValue = 0f;
        }
        log("View measured");
    }

    /**
     * Checks if min or max value on Y axis has changed;
     * If so then it does phantom magic with current Y axis bars;
     */
    private void checkIfMinOrMaxValueChanged() {
        ChartAdapter adapter = this.adapter;
        if (adapter == null) return;

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

    private void animateFadedInChart(ChartData chart) {
        Animator oldAnimator = this.fadedChartAnimator;
        if (oldAnimator != null) oldAnimator.cancel();

        fadedChart = chart;
        fadedChartAlpha = 0f;
        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(ANIM_DURATION);
        animator.setInterpolator(yValueInterpolator);
        animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) { }
            @Override public void onAnimationEnd(Animator animation) {
                finish();
            }
            @Override public void onAnimationCancel(Animator animation) {
                finish();
            }
            @Override public void onAnimationRepeat(Animator animation) { }
            void finish() {
                fadedChart = null;
                fadedChartAlpha = 1f;
                invalidate();
            }
        });
        this.fadedChartAnimator = animator;
        animator.start();
    }

    private void animateFadedOutChart(ChartData chart) {
        Animator oldAnimator = this.fadedChartAnimator;
        if (oldAnimator != null) oldAnimator.cancel();

        fadedChart = chart;
        fadedChartAlpha = 1f;
        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 0f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(ANIM_DURATION);
        animator.setInterpolator(yValueInterpolator);
        animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) { }
            @Override public void onAnimationEnd(Animator animation) {
                finish();
            }
            @Override public void onAnimationCancel(Animator animation) {
                finish();
            }
            @Override public void onAnimationRepeat(Animator animation) { }
            void finish() {
                fadedChart = null;
                fadedChartAlpha = 0f;
                invalidate();
            }
        });
        this.fadedChartAnimator = animator;
        animator.start();
    }

    /**
     * Draws line charts;
     * @param canvas canvas
     */
    protected void drawCharts(Canvas canvas) {
        log("Drawing foreground layer");
        ChartAdapter adapter = this.adapter;
        if (adapter == null)
            return; // early return

        for (int i = 0; i < adapter.getChartCount(); i++) {
            ChartData data = adapter.getChart(i);
            if (!adapter.isVisible(data)) {
                if (fadedChart == null || !fadedChart.equals(data))
                    continue;
            }
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
            if (fadedChart != null && fadedChart.equals(data)) {
                chartPaint.setAlpha((int) (fadedChartAlpha * 255));
            } else chartPaint.setAlpha(255);
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
    public float getStartXPosition() {
        return startXPercentage;
    }

    @Override
    public float getStopXPosition() {
        return stopXPercentage;
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
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setVisible(chart, true);
            checkIfMinOrMaxValueChanged();
            animateFadedInChart(chart);
            invalidate();
        }
    }

    @Override
    public void hide(ChartData chart) {
        // hiding a chart here
        ChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setVisible(chart, false);
            checkIfMinOrMaxValueChanged();
            animateFadedOutChart(chart);
            invalidate();
        }
    }
}
