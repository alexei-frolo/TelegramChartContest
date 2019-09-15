package com.froloapp.chart.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.chart.R;


// This delegate is responsible for animating and drawing Y axis
final class YAxisDelegate {

    private static final String LOG_TAG = "YAxisDelegate";

    private static final long Y_AXIS_ANIM_DURATION = Config.Y_AXIS_ANIM_DURATION;

    private static final Interpolator Y_AXIS_INTERPOLATOR =
            new AccelerateDecelerateInterpolator();

    private static final float DEFAULT_LINE_STROKE_WIDTH_IN_DP = 1.0f;
    private static final float DEFAULT_TEXT_SIZE_IN_SP =
            Config.TEXT_SIZE_IN_SP;

    private final static Property<YAxisDelegate, Float> MIN_Y_VALUE =
            new Property<YAxisDelegate, Float>(float.class, "mMinYValue") {
        @Override public Float get(YAxisDelegate object) {
            return object.mMinYValue;
        }
        @Override public void set(YAxisDelegate object, Float value) {
            object.mMinYValue = value;
            object.requestRedraw();
        }
    };

    private final static Property<YAxisDelegate, Float> MAX_Y_VALUE =
            new Property<YAxisDelegate, Float>(float.class, "mMaxYValue") {
        @Override public Float get(YAxisDelegate object) {
            return object.mMaxYValue;
        }
        @Override public void set(YAxisDelegate object, Float value) {
            object.mMaxYValue = value;
            object.requestRedraw();
        }
    };

    private final static Property<YAxisDelegate, Float> Y_AXIS_ALPHA =
            new Property<YAxisDelegate, Float>(float.class, "mAlpha") {
        @Override public Float get(YAxisDelegate object) {
            return object.mAlpha;
        }
        @Override public void set(YAxisDelegate object, Float value) {
            object.mAlpha = value;
            object.requestRedraw();
        }
    };

    private final AbsChartView mView;

    // count of horizontal lines
    private final int mLineCount = 5;

    // current min and max value on Y axis
    private float mMinYValue;
    private float mMaxYValue;

    private float mTargetMinYValue;
    private float mTargetMaxYValue;

    // current Y axis min value and step
    private float mCurrMinYValue; // from this value, current lines are drawn on Y axis
    private float mCurrYValueStep; // by this step, current lines are drawn on Y axis

    // phantom Y axis min value and step
    private float mPhantomMinYValue; // from this value, phantom lines are drawn on Y axis
    private float mPhantomYValueStep; // by this step, phantom lines are drawn on Y axis

    private float mAlpha = 1f;

    private boolean mIsAnimating;

    private ValueAnimator mAnim;

    private final Animator.AnimatorListener mAnimListener =
            new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator a) {
                    mIsAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator a) {
                    mIsAnimating = false;
                }

                @Override
                public void onAnimationCancel(Animator a) {
                    mIsAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animator a) {
                }
            };

    // Paints tools
    private final Paint mLinePaint;
    private final Paint mTextPaint;

    YAxisDelegate(AbsChartView view) {
        this.mView = view;

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(Misc.dpToPx(DEFAULT_LINE_STROKE_WIDTH_IN_DP, view.getContext()));
        mLinePaint = linePaint;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(Misc.spToPx(DEFAULT_TEXT_SIZE_IN_SP, view.getContext()));
        mTextPaint = textPaint;
    }

    private void requestRedraw() {
        mView.invalidate();
    }

    private long calcAnimDuration(float oldRange, float newRange) {
        if (oldRange != 0 && newRange != 0) {
            float coeff;
            if (newRange > oldRange) {
                coeff = newRange / oldRange;
            } else {
                coeff = oldRange / newRange;
            }

            coeff = coeff * coeff;
            if (coeff > 2.5) {
                coeff = 2.5f;
            }
            return (long) (Y_AXIS_ANIM_DURATION * coeff);
        } else {
            return Y_AXIS_ANIM_DURATION;
        }
    }

    private void drawLineAndText(Canvas canvas,
                                 float value,
                                 int startXCoordinate,
                                 int stopXCoordinate,
                                 float yCoordinate) {
        // line
        canvas.drawLine(
                startXCoordinate,
                yCoordinate,
                stopXCoordinate,
                yCoordinate,
                mLinePaint);

        // text
        String text = Misc.format((int) value);
        canvas.drawText(
                text,
                startXCoordinate,
                yCoordinate - mLinePaint.getStrokeWidth() * 4, // mLinePaint.getStrokeWidth() * 4 adds additional space between line and text
                mTextPaint);
    }

    void loadAttributes(Context context, AttributeSet attrs) {
        int yAxisColor;
        int yAxisTextColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.AbsChartView, 0, 0);
            yAxisColor = typedArray.getColor(R.styleable.AbsChartView_yAxisColor,
                    Color.GRAY);
            yAxisTextColor = typedArray.getColor(R.styleable.AbsChartView_yAxisTextColor,
                    Color.GRAY);
            typedArray.recycle();
        } else {
            yAxisColor = Color.GRAY;
            yAxisTextColor = Color.GRAY;
        }

        mLinePaint.setColor(yAxisColor);
        mTextPaint.setColor(yAxisTextColor);
    }

    void draw(Canvas canvas) {

        int startXCoordinate = mView.getPaddingLeft();
        int stopXCoordinate = mView.getMeasuredWidth() - mView.getPaddingRight();

        int fadeInAlpha = (int) (255 * mAlpha);
        int fadeOutAlpha = (int) (255 * (1 - mAlpha));

        final float lineStrokeWidth = mLinePaint.getStrokeWidth();

        if (mIsAnimating) {
            // Here, we're drawing phantom lines

            mLinePaint.setAlpha(fadeOutAlpha);
            mTextPaint.setAlpha(fadeOutAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = mPhantomMinYValue + i * mPhantomYValueStep;
                float y = CommonHelper.findYCoordinate(
                        mView,
                        mMinYValue,
                        mMaxYValue,
                        value);

                drawLineAndText(
                        canvas,
                        value,
                        startXCoordinate,
                        stopXCoordinate,
                        y - lineStrokeWidth / 2);
            }
        }

        {
            // Here, we're drawing target lines

            // Drawing fading in bars
            mLinePaint.setAlpha(fadeInAlpha);
            mTextPaint.setAlpha(fadeInAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = mCurrMinYValue + i * mCurrYValueStep;
                float y = CommonHelper.findYCoordinate(
                        mView,
                        mMinYValue,
                        mMaxYValue,
                        value);

                drawLineAndText(
                        canvas,
                        value,
                        startXCoordinate,
                        stopXCoordinate,
                        y - lineStrokeWidth / 2);
            }
        }
    }

    void setMaxAndMin(float min, float max, boolean animate) {
        // check min value
        if (min != this.mTargetMinYValue || max != this.mTargetMaxYValue) {
            mTargetMinYValue = min;
            mTargetMaxYValue = max;
            float newRange = max - min;

            this.mPhantomMinYValue = this.mCurrMinYValue;
            this.mPhantomYValueStep = this.mCurrYValueStep;

            this.mCurrMinYValue = min;
            this.mCurrYValueStep = (int) (newRange / (mLineCount));

            float startYAxisAlpha = 0.1f;
            float startMinYValue = mMinYValue;
            float startMaxYValue = mMaxYValue;

            ValueAnimator oldAnimator = mAnim;
            if (oldAnimator != null) {
                oldAnimator.cancel();
            }

            if (animate) {
                boolean calcAnimDuration = false;
                float oldRange = (startMaxYValue - startMinYValue);
                long animDur = calcAnimDuration ? calcAnimDuration(oldRange, newRange) : Y_AXIS_ANIM_DURATION;

                PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, startMinYValue, min);
                PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, startMaxYValue, max);
                PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, startYAxisAlpha, 1f);

                ObjectAnimator newAnimator = ObjectAnimator.ofPropertyValuesHolder(this, h1, h2, h3);
                newAnimator.setInterpolator(Y_AXIS_INTERPOLATOR);
                newAnimator.setDuration(animDur);
                newAnimator.addListener(mAnimListener);
                newAnimator.start();

                mAnim = newAnimator;
            } else {
                mMinYValue = min;
                mMaxYValue = max;
                mAlpha = 1f;
                requestRedraw();
            }
        }
    }

    float getCurrentMinValue() {
        return mMinYValue;
    }

    float getCurrentMaxValue() {
        return mMaxYValue;
    }

    void attach() {
    }

    void measured() {
    }

    void detach() {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
    }
}
