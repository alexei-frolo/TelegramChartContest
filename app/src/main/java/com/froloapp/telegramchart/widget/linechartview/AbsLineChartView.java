package com.froloapp.telegramchart.widget.linechartview;

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
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;


/**
 * Helps to draw line charts, handle start and stop x positions and animate appearing or disappearing of a chart;
 */
public class AbsLineChartView extends View implements LineChartUI {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 15;
    private static final long ANIM_DURATION = 180L; // I think 180 ms is the best duration

    private static final int DEFAULT_X_AXIS_STAMP_COUNT = 5;
    private static final int DEFAULT_Y_AXIS_BAR_COUNT = 5;

    private static final float DEFAULT_CHART_LINE_WIDTH_IN_DP = 1f;
    private static final float DEFAULT_AXIS_LINE_WIDTH_IN_DP = 1f;

    // paint tools
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bufferPath = new Path(); // buffer path to avoid allocating to many paths for multiple charts
    private final Rect stampTextBounds = new Rect(); // here we store bounds for stamp text height
    private float axisStrokeWidth;
    private int footerHeight; // for x axis stamps

    // adapter
    private LineChartAdapter adapter;
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
    private float xAxisStampTextSize;
    private int xAxisStampCount;
    private int xAxisStepCount; // stamp are drawn through this step
    private int xAxisColor;
    private float xAxisAlpha;
    private boolean drawXPhantoms;

    // Animators
    private ValueAnimator yAxisAnimator;

    private final static Property<AbsLineChartView, Float> MIN_Y_VALUE = new Property<AbsLineChartView, Float>(float.class, "minYValue") {
        @Override public Float get(AbsLineChartView object) {
            return object.minYValue;
        }
        @Override public void set(AbsLineChartView object, Float value) {
            object.minYValue = value;
            object.invalidate();
        }
    };
    private final static Property<AbsLineChartView, Float> MAX_Y_VALUE = new Property<AbsLineChartView, Float>(float.class, "maxYValue") {
        @Override public Float get(AbsLineChartView object) {
            return object.maxYValue;
        }
        @Override public void set(AbsLineChartView object, Float value) {
            object.maxYValue = value;
            object.invalidate();
        }
    };
    private final static Property<AbsLineChartView, Float> Y_AXIS_ALPHA = new Property<AbsLineChartView, Float>(float.class, "yAxisAlpha") {
        @Override public Float get(AbsLineChartView object) {
            return object.yAxisAlpha;
        }
        @Override public void set(AbsLineChartView object, Float value) {
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
            //finish();
        }
        @Override public void onAnimationRepeat(Animator animation) { }
        void finish() {
            drawXPhantoms = false;
        }
    };
    private final Interpolator xValueInterpolator = new AccelerateDecelerateInterpolator();

    // Faded in/out chart
    private Line fadedChart = null;
    private float fadedChartAlpha = 0f;
    private ObjectAnimator fadedChartAnimator;
    private final Property<AbsLineChartView, Float> FADED_CHART_ALPHA = new Property<AbsLineChartView, Float>(float.class, "fadedChartAlpha") {
        @Override public Float get(AbsLineChartView object) {
            return object.fadedChartAlpha;
        }
        @Override public void set(AbsLineChartView object, Float value) {
            object.fadedChartAlpha = value;
            invalidate();
        }
    };

    public AbsLineChartView(Context context) {
        this(context, null);
    }

    public AbsLineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsLineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float chartLineWidth;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AbsLineChartView, 0, 0);
            chartLineWidth = typedArray.getDimension(R.styleable.AbsLineChartView_chartStrokeWidth, Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context));
            axisStrokeWidth = typedArray.getDimension(R.styleable.AbsLineChartView_axisStrokeWidth, Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context));
            xAxisStampCount = typedArray.getColor(R.styleable.AbsLineChartView_xAxisStampCount, DEFAULT_X_AXIS_STAMP_COUNT);
            yAxisStampCount = typedArray.getInteger(R.styleable.AbsLineChartView_yAxisStampCount, DEFAULT_Y_AXIS_BAR_COUNT);
            xAxisColor = typedArray.getColor(R.styleable.AbsLineChartView_xAxisColor, Color.LTGRAY);
            yAxisColor = typedArray.getColor(R.styleable.AbsLineChartView_yAxisColor, Color.LTGRAY);
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


        float textSize = Utils.spToPx(DEFAULT_TEXT_HEIGHT_IN_SP, context);
        yAxisTextPaint.setStyle(Paint.Style.FILL);
        yAxisTextPaint.setTextSize(textSize);
        yAxisTextPaint.setColor(yAxisColor);

        // x axis
        xAxisStampCount = 5;
        xAxisTextPaint.setStyle(Paint.Style.FILL);
        xAxisTextPaint.setTextSize(textSize);
        xAxisTextPaint.setColor(xAxisColor);
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

    /*abstract*/ boolean drawFooter() {
        return false;
    }

    /*package-private*/ /*nullable*/ LineChartAdapter getAdapter() {
        return adapter;
    }

    /*package-private*/ /*nullable*/ Line getFadedChart() {
        return fadedChart;
    }

    /*package-private*/ float getFadedChartAlpha() {
        return fadedChartAlpha;
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
        int contentHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - footerHeight;
        float yRelative = (value - minYValue) / (maxYValue - minYValue);
        return (int) (getMeasuredHeight() - getPaddingTop() - footerHeight - yRelative * contentHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Utils.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Utils.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);

        // update important values here
        if (drawFooter()) {
            xAxisTextPaint.getTextBounds("|", 0, 1, stampTextBounds);
            footerHeight = stampTextBounds.height();
        } else {
            footerHeight = 0;
        }

        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            minYValue = adapter.getLocalMinimum(startXPercentage, stopXPercentage);
            maxYValue = adapter.getLocalMaximum(startXPercentage, stopXPercentage);
        } else {
            minYValue = 0f;
            maxYValue = 0f;
        }
    }

    private void checkIfStartOrStopXPositionChanged(float oldStartXPosition, float oldStopXPosition) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        float xPosRange = stopXPercentage - startXPercentage;
        if ((oldStopXPosition - oldStartXPosition) != xPosRange) {
            float timestampCountInRange = adapter.getTimestampCount() * (stopXPercentage - startXPercentage);
            xAxisStepCount = (int) (timestampCountInRange / xAxisStampCount) + 1;
            invalidate();
        }
    }

    /**
     * Checks if min or max value on Y axis has changed;
     * If so then it does phantom magic with current Y axis bars;
     */
    private void checkIfMinOrMaxValueChanged() {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        int minValue = adapter.getLocalMinimum(startXPercentage, stopXPercentage);
        int maxValue = adapter.getLocalMaximum(startXPercentage, stopXPercentage);
        float newRange = maxValue - minValue;

        this.phantomYBarStartValue = this.yBarStartValue;
        this.phantomYBarStep = this.yBarStep;

        this.yBarStartValue = minValue;
        this.yBarStep = (int) (newRange / yAxisStampCount);

        // check min value
        if (minValue != this.minYValue || maxValue != this.maxYValue) {
            ValueAnimator oldAnimator = yAxisAnimator;
            if (oldAnimator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
                else oldAnimator.cancel();
            }

            PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, minValue);
            PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, maxValue);
            PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, 0.1f, 1f);

            ObjectAnimator newAnimator = ObjectAnimator.ofPropertyValuesHolder(this, h1, h2, h3);
            newAnimator.setInterpolator(yValueInterpolator);
            newAnimator.setDuration(ANIM_DURATION);
            newAnimator.addListener(yAxisAnimListener);
            yAxisAnimator = newAnimator;
            newAnimator.start();
        }
    }

    private void animateFadedInChart(final Line chart) {
        Animator oldAnimator = this.fadedChartAnimator;
        if (oldAnimator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
            else oldAnimator.cancel();
        }

        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(ANIM_DURATION);
        animator.setInterpolator(yValueInterpolator);
        animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {
                fadedChart = chart;
            }
            @Override public void onAnimationEnd(Animator animation) {
                finish();
            }
            @Override public void onAnimationCancel(Animator animation) {
                finish();
            }
            @Override public void onAnimationRepeat(Animator animation) { }
            void finish() {
                fadedChart = null;
                invalidate();
            }
        });
        this.fadedChartAnimator = animator;
        animator.start();
    }

    private void animateFadedOutChart(final Line chart) {
        Animator oldAnimator = this.fadedChartAnimator;
        if (oldAnimator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
            else oldAnimator.cancel();
        }

        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 0f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(ANIM_DURATION);
        animator.setInterpolator(yValueInterpolator);
        animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {
                fadedChart = chart;
            }
            @Override public void onAnimationEnd(Animator animation) {
                finish();
            }
            @Override public void onAnimationCancel(Animator animation) {
                finish();
            }
            @Override public void onAnimationRepeat(Animator animation) { }
            void finish() {
                fadedChart = null;
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
        LineChartAdapter adapter = getAdapter();
        if (adapter == null) return;
        // TO DO: draw phantom and actual stamps on X axis

        int index = adapter.getLeftClosestTimestampIndex(startXPercentage);
        index = (index / xAxisStepCount) * xAxisStepCount;

        final int timestampCount = adapter.getTimestampCount();
        final float avTimestampPosXStep = 1f / adapter.getTimestampCount(); // average step
        int timestampIndex = (index / xAxisStepCount) * xAxisStepCount;
        float timestampPosX = adapter.getTimestampRelPositionAt(timestampIndex);

        final float y = getMeasuredHeight() - getPaddingBottom();

        while (timestampIndex < timestampCount) {
            String text = adapter.getXStampText(timestampIndex);
            float x = getXCoor(timestampPosX);
            canvas.drawText(text, x, y, xAxisTextPaint);

            if (timestampPosX > stopXPercentage) {
                break;
            }

            timestampIndex += xAxisStepCount;
            timestampPosX += xAxisStepCount * avTimestampPosXStep;
        }
    }

    private void drawYBarWithStamp(Canvas canvas, float value, int startX, int stopX, Paint barPaint, Paint textPaint) {
        float yFadeOut = getYCoor(value) - (axisStrokeWidth / 2 + 1);
        // bar line
        canvas.drawLine(startX, yFadeOut, stopX, yFadeOut, barPaint);

        // bar text
        String text = adapter.getYStampText((int) value);
        //textPaint.getTextBounds(text, 0, text.length(), stampTextBounds);
        canvas.drawText(text, startX, yFadeOut, textPaint);
    }

    /**
     * Draws Y axis bars with Y stamps;
     * @param canvas canvas
     */
    protected void drawYAxis(Canvas canvas) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        int startX = getPaddingLeft();
        int stopX = getMeasuredWidth() - getPaddingRight();

        int fadeInAlpha = (int) (255 * yAxisAlpha);
        int fadeOutAlpha = (int) (255 * (1 - yAxisAlpha));

        int phantomYBarStartValue = this.phantomYBarStartValue;
        int phantomYBarStep = this.phantomYBarStep;

        if (drawYPhantoms) {
            // Drawing fading out bars
            yAxisPaint.setAlpha(fadeOutAlpha);
            yAxisTextPaint.setAlpha(fadeOutAlpha);
            for (int i = 0; i < yAxisStampCount; i++) {
                float value = phantomYBarStartValue + i * phantomYBarStep;
                drawYBarWithStamp(canvas, value, startX, stopX, yAxisPaint, yAxisTextPaint);
            }
        }

        int yBarStartValue = this.yBarStartValue;
        int yBarStep = this.yBarStep;

        // Drawing fading in bars
        yAxisPaint.setAlpha(fadeInAlpha);
        yAxisTextPaint.setAlpha(fadeInAlpha);
        for (int i = 0; i < yAxisStampCount; i++) {
            float value = yBarStartValue + i * yBarStep;
            drawYBarWithStamp(canvas, value, startX, stopX, yAxisPaint, yAxisTextPaint);
        }
    }

    /**
     * Draws lines;
     * For optimization, the logic of rendering is next:
     * 1) Find the previous timestamp that goes before {@link AbsLineChartView#startXPercentage} value;
     * 2) Create a path of Y values for each chart until reached timestamp that goes after {@link AbsLineChartView#stopXPercentage} value;
     * 3) Draw each path on canvas using appropriate colors;
     * @param canvas canvas
     */
    protected void drawLines(Canvas canvas) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null)
            return; // early return

        final int timestampCount = adapter.getTimestampCount();
        final float avTimestampPosXStep = 1f / adapter.getTimestampCount(); // average step
        final int startTimestampIndex = adapter.getLeftClosestTimestampIndex(startXPercentage);
        final float startTimestampPosX = adapter.getTimestampRelPositionAt(startTimestampIndex);

        for (int i = 0; i < adapter.getLineCount(); i++) {
            Line data = adapter.getLineAt(i);
            if (!adapter.isLineEnabled(data)) {
                if (fadedChart == null || !fadedChart.equals(data))
                    continue;
            }

            float timestampPosX = startTimestampPosX;
            int timestampIndex = startTimestampIndex;
            boolean outsideBounds = false;

            int value = data.getValueAt(timestampIndex);

            bufferPath.reset();
            int xCoor = getXCoor(timestampPosX);
            int yCoor = getYCoor(value);
            bufferPath.moveTo(xCoor, yCoor);

            while (timestampIndex < timestampCount - 1) {
                timestampPosX += avTimestampPosXStep;
                timestampIndex++;

                value = data.getValueAt(timestampIndex);
                xCoor = getXCoor(timestampPosX);
                yCoor = getYCoor(value);
                bufferPath.lineTo(xCoor, yCoor);

                if (outsideBounds) {
                    break;
                }

                if (timestampPosX > stopXPercentage) {
                    // It's enough. No need to draw lines after next timestamp as they will be invisible.
                    // So allow to draw one part more and exit;
                    outsideBounds = true;
                }
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
    public void setAdapter(LineChartAdapter adapter) {
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
            float oldStartXPos = this.startXPercentage;
            checkIfStartOrStopXPositionChanged(oldStartXPos, stopXPercentage);
            this.startXPercentage = p;
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void setStopXPosition(float p) {
        if (stopXPercentage != p) {
            float oldStopXPos = this.stopXPercentage;
            this.stopXPercentage = p;
            checkIfStartOrStopXPositionChanged(startXPercentage, oldStopXPos);
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void setXPositions(float start, float stop) {
        if (this.startXPercentage != start || this.stopXPercentage != stop) {
            float oldStartXPos = this.startXPercentage;
            float oldStopXPos = this.stopXPercentage;
            this.startXPercentage = start;
            this.stopXPercentage = stop;
            checkIfStartOrStopXPositionChanged(oldStartXPos, oldStopXPos);
            checkIfMinOrMaxValueChanged();
            invalidate();
        }
    }

    @Override
    public void show(Line chart) {
        // showing a chart here
        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setLineEnabled(chart, true);
            checkIfMinOrMaxValueChanged();
            animateFadedInChart(chart);
            invalidate();
        }
    }

    @Override
    public void hide(Line chart) {
        // hiding a chart here
        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setLineEnabled(chart, false);
            checkIfMinOrMaxValueChanged();
            animateFadedOutChart(chart);
            invalidate();
        }
    }
}