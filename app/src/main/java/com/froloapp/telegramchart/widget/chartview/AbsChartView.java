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
import android.graphics.Rect;
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
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final long ANIM_DURATION = 300L;

    private static final int DEFAULT_X_AXIS_STAMP_COUNT = 5;
    private static final int DEFAULT_Y_AXIS_BAR_COUNT = 5;

    private static final float DEFAULT_CHART_LINE_WIDTH_IN_DP = 1f;
    private static final float DEFAULT_AXIS_LINE_WIDTH_IN_DP = 1f;

    private ChartAdapter adapter;

    // paint tools
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bufferPath = new Path(); // buffer path to avoid allocating to many paths for multiple charts
    private final Rect stampTextBounds = new Rect(); // here we store bounds for stamp text height
    private float axisStrokeWidth;

    // current start and stop positions on X Axis in percentage(from 0 to 1)
    private float startXPercentage;
    private float stopXPercentage;

    // current min and max value on Y axis
    private float minYValue;
    private float maxYValue;

    /* *********************************
     *** Y AXIS PROPERTIES INTERFACE ***
     **********************************/
    private int yAxisStampCount;
    private int yAxisColor;
    private float yAxisAlpha;
    // actual stamps
    private int yBarStartValue; // from this value, y axis bars are drawn
    private int yBarStep; // by this step, y axis bars are drawn
    // phantom stamps
    private boolean drawYPhantoms;
    private int phantomYBarStartValue; // from this value, phantom y axis bars are drawn
    private int phantomYBarStep; // by this step, y axis bars are drawn

    /* *********************************
     *** X AXIS PROPERTIES INTERFACE ***
     **********************************/
    private int xAxisStampCount;
    private int xAxisStepCount; // stamp are drawn through this step
    private int xAxisColor;
    private float xAxisAlpha;
    private boolean drawXPhantoms;

    // Animators
    private ValueAnimator yAxisAnimator;

    private final static Property<AbsChartView, Float> MIN_Y_VALUE = new Property<AbsChartView, Float>(float.class, "minYValue") {
        @Override public Float get(AbsChartView object) {
            return object.minYValue;
        }
        @Override public void set(AbsChartView object, Float value) {
            object.minYValue = value;
            object.invalidate();
        }
    };
    private final static Property<AbsChartView, Float> MAX_Y_VALUE = new Property<AbsChartView, Float>(float.class, "maxYValue") {
        @Override public Float get(AbsChartView object) {
            return object.maxYValue;
        }
        @Override public void set(AbsChartView object, Float value) {
            object.maxYValue = value;
            object.invalidate();
        }
    };
    private final static Property<AbsChartView, Float> Y_AXIS_ALPHA = new Property<AbsChartView, Float>(float.class, "yAxisAlpha") {
        @Override public Float get(AbsChartView object) {
            return object.yAxisAlpha;
        }
        @Override public void set(AbsChartView object, Float value) {
            object.yAxisAlpha = value;
            object.invalidate();
        }
    };
    private final ValueAnimator.AnimatorListener yAxisAnimListener = new Animator.AnimatorListener() {
        @Override public void onAnimationStart(Animator animation) {
            drawYPhantoms = true;
        }
        @Override public void onAnimationEnd(Animator animation) {
            finish();
        }
        @Override public void onAnimationCancel(Animator animation) {
            //finish();
        }
        @Override public void onAnimationRepeat(Animator animation) { }
        void finish() {
            drawYPhantoms = false;
            yAxisAlpha = 1f;
            invalidate();
        }
    };
    private final Interpolator yValueInterpolator = new AccelerateDecelerateInterpolator();

    private final ValueAnimator.AnimatorListener xAxisAnimListener = new Animator.AnimatorListener() {
        @Override public void onAnimationStart(Animator animation) {
            drawXPhantoms = true;
        }
        @Override public void onAnimationEnd(Animator animation) {
            finish();
        }
        @Override public void onAnimationCancel(Animator animation) {
            finish();
        }
        @Override public void onAnimationRepeat(Animator animation) { }
        void finish() {
            drawXPhantoms = false;
            xAxisAlpha = 1f;
            invalidate();
        }
    };
    private final Interpolator xValueInterpolator = new AccelerateDecelerateInterpolator();

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
            chartLineWidth = typedArray.getDimension(R.styleable.AbsChartView_chartStrokeWidth, Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context));
            axisStrokeWidth = typedArray.getDimension(R.styleable.AbsChartView_axisStrokeWidth, Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context));
            xAxisStampCount = typedArray.getColor(R.styleable.AbsChartView_xAxisStampCount, DEFAULT_X_AXIS_STAMP_COUNT);
            yAxisStampCount = typedArray.getInteger(R.styleable.AbsChartView_yAxisStampCount, DEFAULT_Y_AXIS_BAR_COUNT);
            xAxisColor = typedArray.getColor(R.styleable.AbsChartView_xAxisColor, Color.LTGRAY);
            yAxisColor = typedArray.getColor(R.styleable.AbsChartView_yAxisColor, Color.LTGRAY);
            typedArray.recycle();
        } else {
            chartLineWidth = Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context);
            axisStrokeWidth = Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context);
            xAxisStampCount = DEFAULT_X_AXIS_STAMP_COUNT;
            yAxisStampCount = DEFAULT_Y_AXIS_BAR_COUNT;
            xAxisColor = Color.LTGRAY;
            yAxisColor = Color.LTGRAY;
        }

        // chart paint
        chartPaint.setStrokeWidth(chartLineWidth);

        // axis paint
        yAxisPaint.setStrokeWidth(axisStrokeWidth);
        yAxisPaint.setStyle(Paint.Style.STROKE);
        // color for y axis bars and x axis stamps
        yAxisPaint.setColor(yAxisColor);

        yAxisTextPaint.setStyle(Paint.Style.FILL);
        float textSize = Utils.spToPx(DEFAULT_TEXT_HEIGHT_IN_SP, context);
        yAxisTextPaint.setTextSize(textSize);
        yAxisTextPaint.setColor(yAxisColor);
    }

    public int getXAxisColor() {
        return xAxisColor;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // We need to cancel all animations here

        ValueAnimator a1 = yAxisAnimator;
        if (a1 != null) a1.cancel();

        ValueAnimator a2 = fadedChartAnimator;
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

    /*package-private*/ /*nullable*/ ChartAdapter getAdapter() {
        return adapter;
    }

    /*package-private*/ /*nullable*/ ChartData getFadedChart() {
        return fadedChart;
    }

    /*package-private*/ float getFadedChartAlpha() {
        return fadedChartAlpha;
    }

    /*package-private*/ float getMinYValue() {
        return minYValue;
    }

    /*package-private*/ float getMaxYValue() {
        return maxYValue;
    }

    /*package-private*/ float getXPosition(float x) {
        float rel = (x - getPaddingLeft()) / (getMeasuredWidth() - getPaddingRight());
        return startXPercentage + (stopXPercentage - startXPercentage) * rel;
    }

    /*package-private*/ int getXCoor(float p) {
        int contentWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        float xRelative = ((float) (p - startXPercentage)) / (stopXPercentage - startXPercentage);
        return (int) (getPaddingLeft() + xRelative * contentWidth);
    }

    /*package-private*/ int getYCoor(float value) {
        int contentHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        float yRelative = (value - minYValue) / (maxYValue - minYValue);
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
        float newRange = maxValue - minValue;

        this.yAxisAlpha = 0.1f;

        this.phantomYBarStartValue = this.yBarStartValue;
        this.phantomYBarStep = this.yBarStep;

        this.yBarStartValue = minValue;
        this.yBarStep = (int) (newRange / yAxisStampCount);

        // check min value
        if (minValue != this.minYValue || maxValue != this.maxYValue) {
            //log("Min Y value changed. Starting animator");
            ValueAnimator oldAnimator = yAxisAnimator;
            if (oldAnimator != null) oldAnimator.cancel();

            PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, minValue);
            PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, maxValue);
            PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, 1f);

            ObjectAnimator newAnimator = ObjectAnimator.ofPropertyValuesHolder(this, h1, h2, h3);
            newAnimator.setInterpolator(yValueInterpolator);
            newAnimator.setDuration(ANIM_DURATION);
            newAnimator.addListener(yAxisAnimListener);
            yAxisAnimator = newAnimator;
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
     * Draws X axis stamps;
     * @param canvas canvas
     */
    protected void drawXAxis(Canvas canvas) {
        //log("Drawing X axis");
        ChartAdapter adapter = getAdapter();
        if (adapter == null) return;
        // TO DO: draw phantom and actual stamps on X axis
    }

    /**
     * Draws Y axis bars with Y stamps;
     * @param canvas canvas
     */
    protected void drawYAxis(Canvas canvas) {
        //log("Drawing Y axis");
        ChartAdapter adapter = getAdapter();
        if (adapter == null) return;

        int startX = getPaddingLeft();
        int stopX = getMeasuredWidth() - getPaddingRight();

        int fadeInAlpha = (int) (255 * yAxisAlpha);
        int fadeOutAlpha = (int) (255 * (1 - yAxisAlpha));
        //int fadeInAlpha = (int) (255 * (1 - yAxisAlpha));
        //int fadeOutAlpha = (int) (255 * yAxisAlpha);

        int phantomYBarStartValue = this.phantomYBarStartValue;
        int phantomYBarStep = this.phantomYBarStep;

        if (drawYPhantoms) {
            // Drawing fading out bars
            yAxisPaint.setAlpha(fadeOutAlpha);
            yAxisTextPaint.setAlpha(fadeOutAlpha);
            for (int i = 0; i < yAxisStampCount; i++) {
                float value = phantomYBarStartValue + i * phantomYBarStep;
                float yFadeOut = getYCoor(value) - (axisStrokeWidth / 2 + 1);
                // bar line
                canvas.drawLine(startX, yFadeOut, stopX, yFadeOut, yAxisPaint);

                // bar text
                String text = adapter.getYBarText((int) value);
                yAxisTextPaint.getTextBounds(text, 0, text.length(), stampTextBounds);
                canvas.drawText(text, startX, yFadeOut, yAxisTextPaint);
            }
        }

        int yBarStartValue = this.yBarStartValue;
        int yBarStep = this.yBarStep;

        // Drawing fading in bars
        yAxisPaint.setAlpha(fadeInAlpha);
        yAxisTextPaint.setAlpha(fadeInAlpha);
        for (int i = 0; i < yAxisStampCount; i++) {
            //float y = getMeasuredHeight() - getPaddingBottom() - i * yAxisBarStep - (axisStrokeWidth / 2 + 1);
            float value = yBarStartValue + i * yBarStep;
            float yFadeIn = getYCoor(value) - (axisStrokeWidth / 2 + 1);
            canvas.drawLine(startX, yFadeIn, stopX, yFadeIn, yAxisPaint);

            // bar text
            String text = adapter.getYBarText((int) value);
            yAxisTextPaint.getTextBounds(text, 0, text.length(), stampTextBounds);
            canvas.drawText(text, startX, yFadeIn, yAxisTextPaint);
        }
    }

    /**
     * Draws charts;
     * For optimization, the logic of rendering is next:
     * 1) Find the previous timestamp that goes before {@link AbsChartView#startXPercentage} value;
     * 2) Create a path of Y values for each chart until reached timestamp that goes after {@link AbsChartView#stopXPercentage} value;
     * 3) Draw each path on canvas using appropriate colors;
     * @param canvas canvas
     */
    protected void drawCharts(Canvas canvas) {
        //log("Drawing foreground layer");
        ChartAdapter adapter = this.adapter;
        if (adapter == null)
            return; // early return

        for (int i = 0; i < adapter.getChartCount(); i++) {
            ChartData data = adapter.getChart(i);
            if (!adapter.isVisible(data)) {
                if (fadedChart == null || !fadedChart.equals(data))
                    continue;
            }

            // Find the first timestamp;
            long timestamp = adapter.getPreviousTimestamp(startXPercentage);
            float timestampPosX = adapter.getPreviousTimestampPosition(startXPercentage);

            int value = data.getValue(timestamp);

            bufferPath.reset();
            int xCoor = getXCoor(timestampPosX);
            int yCoor = getYCoor(value);
            bufferPath.moveTo(xCoor, yCoor);

            while (adapter.hasNextTimestamp(timestamp)) {
                timestamp = adapter.getNextTimestamp(timestamp);
                timestampPosX = adapter.getNextTimestampPosition(timestampPosX);

                value = data.getValue(timestamp);
                xCoor = getXCoor(timestampPosX);
                yCoor = getYCoor(value);
                bufferPath.lineTo(xCoor, yCoor);

                if (timestampPosX > stopXPercentage)
                    break; // It's enough. No need to draw lines after this timestamp as they will be invisible
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
