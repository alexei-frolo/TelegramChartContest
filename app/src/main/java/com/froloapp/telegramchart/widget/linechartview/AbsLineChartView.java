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
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
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
public class AbsLineChartView extends View implements LineChartUI {
    // static
    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;
    private static final int DEFAULT_TEXT_HEIGHT_IN_SP = 12;
    private static final long Y_AXIS_ANIM_DURATION = 250L;
    private static final long Y_AXIS_ANIM_DUR = 150L;
    private static final long X_AXIS_ANIM_DURATION = 250L;

    private static final int DEFAULT_Y_AXIS_BAR_COUNT = 5;

    private static final float DEFAULT_CHART_LINE_WIDTH_IN_DP = 1f;
    private static final float DEFAULT_AXIS_LINE_WIDTH_IN_DP = 1f;

    // paint tools
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yAxisPaint = new Paint(); // Paint.ANTI_ALIAS_FLAG???
    private final Paint yAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] bufferLinePoints; // for collecting (x; y) coors of a chart line to draw
    private final Rect buffTextBounds = new Rect(); // here we store bounds for stamp text height
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

    private float targetMinYValue;
    private float targetMaxYValue;

    /* *********************************
     *** Y AXIS PROPERTIES INTERFACE ***
     **********************************/
    private int yAxisStampCount;
    private int yAxisColor;
    private int yAxisTextColor;
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
    // IMPORTANT: xAxisStampMaxCount / xAxisStampMinCount = 2 ALWAYS!!!
    private final float xAxisStampMaxCount = 6;
    private final float xAxisStampMinCount = 3;
    private int xAxisStampIndexStepCount = 5; // stamp are drawn through this step
    private int phantomXAxisStampIndexStepCount; // phantom stamp are drawn through this step
    private int xAxisColor;
    private int xAxisTextColor;
    private float xAxisAlpha;
    private boolean drawXPhantoms;

    // Animators
    private ValueAnimator yAxisAnimator;
    private ValueAnimator xAxisAnimator;

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
    private final static Property<AbsLineChartView, Float> X_AXIS_ALPHA = new Property<AbsLineChartView, Float>(float.class, "xAxisAlpha") {
        @Override public Float get(AbsLineChartView object) {
            return object.xAxisAlpha;
        }
        @Override public void set(AbsLineChartView object, Float value) {
            object.xAxisAlpha = value;
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
            yAxisStampCount = typedArray.getInteger(R.styleable.AbsLineChartView_yAxisStampCount, DEFAULT_Y_AXIS_BAR_COUNT);
            xAxisColor = typedArray.getColor(R.styleable.AbsLineChartView_xAxisColor, Color.LTGRAY);
            xAxisTextColor = typedArray.getColor(R.styleable.AbsLineChartView_xAxisTextColor, Color.LTGRAY);
            yAxisColor = typedArray.getColor(R.styleable.AbsLineChartView_yAxisColor, Color.LTGRAY);
            yAxisTextColor = typedArray.getColor(R.styleable.AbsLineChartView_yAxisTextColor, Color.LTGRAY);
            typedArray.recycle();
        } else {
            chartLineWidth = Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context);
            axisStrokeWidth = Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context);
            yAxisStampCount = DEFAULT_Y_AXIS_BAR_COUNT;
            xAxisColor = Color.LTGRAY;
            xAxisTextColor = Color.LTGRAY;
            yAxisColor = Color.LTGRAY;
            yAxisTextColor = Color.LTGRAY;
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
        yAxisTextPaint.setColor(yAxisTextColor);

        // x axis
        xAxisTextPaint.setStyle(Paint.Style.FILL);
        xAxisTextPaint.setTextSize(textSize);
        xAxisTextPaint.setColor(xAxisTextColor);
    }

    public int getXAxisColor() {
        return xAxisColor;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // We need to cancel all animations here

        ValueAnimator a1 = xAxisAnimator;
        if (a1 != null) a1.cancel();

        ValueAnimator a2 = yAxisAnimator;
        if (a2 != null) a2.cancel();

        ValueAnimator a3 = fadedChartAnimator;
        if (a3 != null) a3.cancel();
    }

    /* *********************************
     ********** HELPER METHODS *********
     ***********************************/

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("AbsLineChartView", msg);
        }
    }

    /*abstract*/ boolean drawFooter() {
        return false;
    }

    /*abstract*/ int getFooterHeight() {
        return footerHeight;
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
        float rel = (x - getPaddingLeft()) / (getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
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
            String test = "Mar 1.";
            xAxisTextPaint.getTextBounds(test, 0, test.length(), buffTextBounds);
            footerHeight = buffTextBounds.height() + 5; // + 5 to make a margin between stamps and the first y bar
        } else {
            footerHeight = 0;
        }

        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            LineChartAdapter.MinMaxValue minMaxValue = adapter.getLocalMinMax(startXPercentage, stopXPercentage);
            minYValue = minMaxValue.min;
            maxYValue = minMaxValue.max;
        } else {
            minYValue = 0f;
            maxYValue = 0f;
        }
        targetMinYValue = minYValue;
        targetMaxYValue = maxYValue;

        checkIfXAxisStepCountChanged(false);
    }

    private void checkIfXAxisStepCountChanged(boolean animate) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        boolean changed = false;
        if (xAxisStampIndexStepCount < 1) {
            xAxisStampIndexStepCount = 1; // invalidating
            changed = true;
        }
        float timestampCountInRange = adapter.getTimestampCount() * (stopXPercentage - startXPercentage) / xAxisStampIndexStepCount;
        if (timestampCountInRange > xAxisStampMaxCount) {
            phantomXAxisStampIndexStepCount = xAxisStampIndexStepCount > 0 ? xAxisStampIndexStepCount : 1;
            while (timestampCountInRange > xAxisStampMaxCount) {
                xAxisStampIndexStepCount *= 2;
                timestampCountInRange = adapter.getTimestampCount() * (stopXPercentage - startXPercentage) / xAxisStampIndexStepCount;
            }
            changed = true;
        } else if (timestampCountInRange < xAxisStampMinCount) {
            while (timestampCountInRange < xAxisStampMinCount) {
                phantomXAxisStampIndexStepCount = xAxisStampIndexStepCount > 0 ? xAxisStampIndexStepCount : 1;
                xAxisStampIndexStepCount /= 2;
                if (xAxisStampIndexStepCount < 1) {
                    xAxisStampIndexStepCount = 1;
                    break;
                }
                timestampCountInRange = adapter.getTimestampCount() * (stopXPercentage - startXPercentage) / xAxisStampIndexStepCount;
            }
            changed = true;
        }

        if (changed) {
            ValueAnimator oldAnimator = xAxisAnimator;
            if (oldAnimator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
                else oldAnimator.cancel();
            }

            if (animate) {
                PropertyValuesHolder h = PropertyValuesHolder.ofFloat(X_AXIS_ALPHA, 0.1f, 1f);
                ObjectAnimator newAnimator = ObjectAnimator.ofPropertyValuesHolder(this, h);
                newAnimator.setDuration(X_AXIS_ANIM_DURATION);
                newAnimator.setInterpolator(xValueInterpolator);
                newAnimator.addListener(xAxisAnimListener);
                xAxisAnimator = newAnimator;
                newAnimator.start();
            } else {
                xAxisAlpha = 1f;
                invalidate();
            }
        }
    }

    // calculates appropriate anim duration for the given old and new Y value ranges
    private long getYAxisAnimDuration(float oldYValueRange, float newYValueRange) {
        if (oldYValueRange != 0 && newYValueRange != 0) {
            float coeff;
            if (newYValueRange > oldYValueRange) {
                coeff = newYValueRange / oldYValueRange;
            } else {
                coeff = oldYValueRange / newYValueRange;
            }

            coeff = coeff * coeff;
            if (coeff > 2.5) {
                coeff = 2.5f;
            }
            return (long) (Y_AXIS_ANIM_DUR * coeff);
        } else {
            return Y_AXIS_ANIM_DURATION;
        }
    }

    /**
     * Checks if min or max value on Y axis has changed;
     * If so then it does phantom magic with current Y axis bars;
     * @param animate if transition needs to be animated
     * @param calcAnimDuration true if animation duration should be calculated, false - to use default duration (ignored if @param animate = false)
     */
    private void checkIfMinOrMaxValueChanged(boolean animate, boolean calcAnimDuration) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        LineChartAdapter.MinMaxValue minMaxValue = adapter.getLocalMinMax(startXPercentage, stopXPercentage);
        final int newMinValue = minMaxValue.min;
        final int newMaxValue = minMaxValue.max;

        // check min value
        if (newMinValue != this.targetMinYValue || newMaxValue != this.targetMaxYValue) {
            targetMinYValue = newMinValue;
            targetMaxYValue = newMaxValue;
            float newRange = newMaxValue - newMinValue;

            this.phantomYBarStartValue = this.yBarStartValue;
            this.phantomYBarStep = this.yBarStep;

            this.yBarStartValue = newMinValue;
            this.yBarStep = (int) (newRange / (yAxisStampCount));

            float startYAxisAlpha = 0.1f;
            float startMinYValue = minYValue;
            float startMaxYValue = maxYValue;

            ValueAnimator oldAnimator = yAxisAnimator;
            if (oldAnimator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
                else oldAnimator.cancel();
            }

            if (animate) {
                float oldRange = (startMaxYValue - startMinYValue);
                long animDur = calcAnimDuration ? getYAxisAnimDuration(oldRange, newRange) : Y_AXIS_ANIM_DURATION;

                PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, startMinYValue, newMinValue);
                PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, startMaxYValue, newMaxValue);
                PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, startYAxisAlpha, 1f);

                ObjectAnimator newAnimator = ObjectAnimator.ofPropertyValuesHolder(AbsLineChartView.this, h1, h2, h3);
                newAnimator.setInterpolator(yValueInterpolator);
                newAnimator.setDuration(animDur);
                newAnimator.addListener(yAxisAnimListener);
                yAxisAnimator = newAnimator;
                newAnimator.start();
            } else {
                minYValue = newMinValue;
                maxYValue = newMaxValue;
                yAxisAlpha = 1f;
                invalidate();
            }
        }
    }

    private void animateFadedInChart(final Line chart) {
        Animator oldAnimator = this.fadedChartAnimator;
        if (oldAnimator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) oldAnimator.pause();
            else oldAnimator.cancel();
        }

        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 0f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(Y_AXIS_ANIM_DURATION);
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

        PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(FADED_CHART_ALPHA, 1f, 0f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(this, holder);
        animator.setDuration(Y_AXIS_ANIM_DURATION);
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

    private void drawXAxisStampsNoTransition(Canvas canvas) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        final float y = getMeasuredHeight() - getPaddingBottom();

        final int timestampCount = adapter.getTimestampCount();
        final float avTimestampPosXStep = 1f / adapter.getTimestampCount(); // average step

        int timestampIndex = adapter.getLeftClosestTimestampIndex(startXPercentage);
        timestampIndex = (timestampIndex / xAxisStampIndexStepCount) * xAxisStampIndexStepCount; // normalize
        float timestampPosX = adapter.getTimestampRelPositionAt(timestampIndex);

        xAxisTextPaint.setAlpha(255);
        while (timestampIndex < timestampCount) {
            String text = adapter.getXStampTextAt(timestampIndex);
            float x = getXCoor(timestampPosX);
            canvas.drawText(text, x, y, xAxisTextPaint);
            if (timestampPosX > stopXPercentage) {
                break;
            }
            timestampIndex += xAxisStampIndexStepCount;
            timestampPosX += xAxisStampIndexStepCount * avTimestampPosXStep;
        }
    }

    private void drawXAxisStampsTransition(Canvas canvas) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null) return;

        final float y = getMeasuredHeight() - getPaddingBottom();

        final int timestampCount = adapter.getTimestampCount();
        final float avTimestampPosXStep = 1f / adapter.getTimestampCount(); // average step

        boolean fadeIn = xAxisStampIndexStepCount < phantomXAxisStampIndexStepCount; // fade int if true, fade out - otherwise
        int smallStep = fadeIn ? xAxisStampIndexStepCount : phantomXAxisStampIndexStepCount;
        int bigStep = fadeIn ? phantomXAxisStampIndexStepCount : xAxisStampIndexStepCount;

        int timestampIndex = adapter.getLeftClosestTimestampIndex(startXPercentage);
        timestampIndex = (timestampIndex / smallStep) * smallStep; // normalize
        float timestampPosX = adapter.getTimestampRelPositionAt(timestampIndex);

        int alpha = fadeIn ? (int) (xAxisAlpha * 255) : (int) ((1 - xAxisAlpha) * 255);

        while (timestampIndex < timestampCount) {
            String text = adapter.getXStampTextAt(timestampIndex);
            float x = getXCoor(timestampPosX);

            if (timestampIndex % bigStep != 0) { // means phantom
                xAxisTextPaint.setAlpha(alpha);
            } else {
                xAxisTextPaint.setAlpha(255);
            }

            canvas.drawText(text, x, y, xAxisTextPaint);

            if (timestampPosX > stopXPercentage) {
                break;
            }
            timestampIndex += smallStep;
            timestampPosX += smallStep * avTimestampPosXStep;
        }
    }

    /**
     * Draws X axis stamps with or without transition;
     * @param canvas canvas
     */
    protected void drawXAxis(Canvas canvas) {
        if (drawXPhantoms) {
            drawXAxisStampsTransition(canvas);
        } else {
            drawXAxisStampsNoTransition(canvas);
        }
    }

    private void drawYBarWithStamp(Canvas canvas, float value, int startX, int stopX, float y, Paint barPaint, Paint textPaint) {
        // bar line
        canvas.drawLine(startX, y, stopX, y, barPaint);

        // bar text
        String text = adapter.getYStampText((int) value);
        canvas.drawText(text, startX, y - 5, textPaint); // y - 5 to make a margin between stamp and bar
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
                float y = getYCoor(value) - axisStrokeWidth / 2;
                drawYBarWithStamp(canvas, value, startX, stopX, y, yAxisPaint, yAxisTextPaint);
            }
        }

        int yBarStartValue = this.yBarStartValue;
        int yBarStep = this.yBarStep;

        // Drawing fading in bars
        yAxisPaint.setAlpha(fadeInAlpha);
        yAxisTextPaint.setAlpha(fadeInAlpha);
        for (int i = 0; i < yAxisStampCount; i++) {
            float value = yBarStartValue + i * yBarStep;
            float y = getYCoor(value) - axisStrokeWidth / 2;
            drawYBarWithStamp(canvas, value, startX, stopX, y, yAxisPaint, yAxisTextPaint);
        }
    }

    /**
     * For optimization, the logic of rendering is next:
     * 1) Find the previous timestamp that goes before {@link AbsLineChartView#startXPercentage} value;
     * 2) Collect x and y coordinates of each timestamp into a {@link AbsLineChartView#bufferLinePoints}
     * until reached timestamp that goes after {@link AbsLineChartView#stopXPercentage} value;
     * 3) Draw each line on canvas using appropriate colors;
     * @param canvas canvas
     */
    protected void drawLines(Canvas canvas) {
        LineChartAdapter adapter = this.adapter;
        if (adapter == null)
            return; // early return

        final int timestampCount = adapter.getTimestampCount();
        final int startTimestampIndex = adapter.getLeftClosestTimestampIndex(startXPercentage);
        final float startTimestampPosX = adapter.getTimestampRelPositionAt(startTimestampIndex);

        for (int i = 0; i < adapter.getLineCount(); i++) {
            Line data = adapter.getLineAt(i);
            if (!adapter.isLineEnabled(data)) {
                if (fadedChart == null || !fadedChart.equals(data))
                    continue;
            }

            int timestampIndex = startTimestampIndex;
            float timestampPosX = startTimestampPosX;
            boolean outsideBounds = false; // make it true when the render-loop must break

            float value = data.getValueAt(timestampIndex);

            // preparing paint tool
            chartPaint.setColor(data.getColor());
            if (fadedChart != null && fadedChart.equals(data)) {
                chartPaint.setAlpha((int) (fadedChartAlpha * 255));
            } else chartPaint.setAlpha(255);
            chartPaint.setStyle(Paint.Style.STROKE);

            int xCoor = getXCoor(timestampPosX);
            int yCoor = getYCoor(value);
            int k = 0;
            bufferLinePoints[k++] = xCoor;
            bufferLinePoints[k++] = yCoor;

            while (timestampIndex < timestampCount - 1) {
                timestampIndex++;
                timestampPosX = adapter.getTimestampRelPositionAt(timestampIndex); // i think it could be optimized

                value = data.getValueAt(timestampIndex);
                xCoor = getXCoor(timestampPosX);
                yCoor = getYCoor(value);
                //canvas.drawLine(xCoor, yCoor, nextXCoor, nextYCoor, chartPaint);
                bufferLinePoints[k++] = xCoor;
                bufferLinePoints[k++] = yCoor;

                if (outsideBounds) {
                    break;
                }

                if (timestampPosX > stopXPercentage) {
                    // It's enough. No need to draw lines after next timestamp as they will be invisible.
                    // So allow to draw one part more and exit;
                    outsideBounds = true;
                }

                bufferLinePoints[k] = bufferLinePoints[k++ - 2];
                bufferLinePoints[k] = bufferLinePoints[k++ - 2];
            }

            canvas.drawLines(bufferLinePoints, 0, k - 1, chartPaint);
        }
    }

    /* *********************************
     ********* PUBLIC INTERFACE ********
     **********************************/

    @Override
    public void setAdapter(LineChartAdapter adapter, boolean animate) {
        this.adapter = adapter;
        if (adapter != null) {
            bufferLinePoints = new float[adapter.getTimestampCount() * 4];
        }
        checkIfXAxisStepCountChanged(animate);
        checkIfMinOrMaxValueChanged(animate, false);
        invalidate();
    }

    @Override
    public void setXPositions(float start, float stop, boolean animate) {
        if (this.startXPercentage != start || this.stopXPercentage != stop) {
            this.startXPercentage = start;
            this.stopXPercentage = stop;
            checkIfXAxisStepCountChanged(animate);
            checkIfMinOrMaxValueChanged(animate, true);
            invalidate();
        }
    }

    @Override
    public void show(Line chart, boolean animate) {
        // showing a chart here
        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setLineEnabled(chart, true);
            checkIfMinOrMaxValueChanged(animate, false);
            animateFadedInChart(chart);
            invalidate();
        }
    }

    @Override
    public void hide(Line chart, boolean animate) {
        // hiding a chart here
        LineChartAdapter adapter = this.adapter;
        if (adapter != null) {
            adapter.setLineEnabled(chart, false);
            // if (adapter.hasEnabledLines()) // maybe do not check for new local min and max if adapter has no enabled lines
            checkIfMinOrMaxValueChanged(animate, false);
            animateFadedOutChart(chart);
            invalidate();
        }
    }

    /* *********************************
     ****** SAVING INSTANCE STATE ******
     **********************************/

    static class SavedState extends BaseSavedState {
        private float startXPercentage;
        private float stopXPercentage;
        private float minYValue;
        private float maxYValue;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            startXPercentage = in.readFloat();
            stopXPercentage = in.readFloat();
            minYValue = in.readFloat();
            maxYValue = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(startXPercentage);
            out.writeFloat(stopXPercentage);
            out.writeFloat(minYValue);
            out.writeFloat(maxYValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void dumpState() {
        char divider = '\n';
        String stateTxt = new StringBuilder()
                .append("startXPercentage").append('=').append(startXPercentage).append(divider)
                .append("stopXPercentage").append('=').append(stopXPercentage).append(divider)
                .append("minYValue").append('=').append(minYValue).append(divider)
                .append("maxYValue").append('=').append(maxYValue).append(divider)
                .toString();
        log("Dumping:\n" + stateTxt);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        dumpState();

        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.startXPercentage = startXPercentage;
        ss.stopXPercentage = stopXPercentage;
        ss.minYValue = minYValue;
        ss.maxYValue = maxYValue;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        startXPercentage = ss.startXPercentage;
        stopXPercentage = ss.stopXPercentage;
        minYValue = ss.minYValue;
        maxYValue = ss.maxYValue;

        dumpState();

        checkIfXAxisStepCountChanged(false);
        checkIfMinOrMaxValueChanged(false, false);
    }
}
