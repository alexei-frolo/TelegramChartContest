package com.froloapp.telegramchart.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.Log;
import android.util.Property;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.BuildConfig;


final class YAxisHelper {

    private static final String LOG_TAG = "YAxisHelper";

    private static final long Y_AXIS_ANIM_DURATION = 2000;

    private static final Interpolator Y_AXIS_INTERPOLATOR =
            new FastOutLinearInInterpolator();

    private static final float DEFAULT_LINE_STROKE_WIDTH_IN_DP = 2f;
    private static final float DEFAULT_TEXT_SIZE_IN_SP = 16f;

    private final static Property<YAxisHelper, Float> MIN_Y_VALUE =
            new Property<YAxisHelper, Float>(float.class, "mTargetYMin") {
        @Override public Float get(YAxisHelper object) {
            return object.mTargetYMin;
        }
        @Override public void set(YAxisHelper object, Float value) {
            object.mTargetYMin = value;
            object.requestRedraw();
        }
    };

    private final static Property<YAxisHelper, Float> MAX_Y_VALUE =
            new Property<YAxisHelper, Float>(float.class, "mTargetYMax") {
        @Override public Float get(YAxisHelper object) {
            return object.mTargetYMax;
        }
        @Override public void set(YAxisHelper object, Float value) {
            object.mTargetYMax = value;
            object.requestRedraw();
        }
    };

    private final static Property<YAxisHelper, Float> Y_AXIS_ALPHA =
            new Property<YAxisHelper, Float>(float.class, "mAlpha") {
        @Override public Float get(YAxisHelper object) {
            return object.mAlpha;
        }
        @Override public void set(YAxisHelper object, Float value) {
            object.mAlpha = value;
            object.requestRedraw();
        }
    };

    private final AbsChartView mView;

    private final int mLineCount = 5;

    // actual Y min and max
    private float mTargetYMin;
    private float mTargetYMax;

    // old Y min and max
    private float mPhantomYMin = 0;
    private float mPhantomYMax = 0;

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

    YAxisHelper(AbsChartView view) {
        this.mView = view;

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(Utils.dpToPx(DEFAULT_LINE_STROKE_WIDTH_IN_DP, view.getContext()));
        mLinePaint = linePaint;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(Utils.spToPx(DEFAULT_TEXT_SIZE_IN_SP, view.getContext()));
        mTextPaint = textPaint;
    }

    private void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg);
    }

    private void requestRedraw() {
        mView.invalidate();
    }

    private long calcAnimDuration(float oldRange, float newRange) {
        return Y_AXIS_ANIM_DURATION;
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
        String text = Utils.format((int) value);
        canvas.drawText(
                text,
                startXCoordinate,
                yCoordinate - 5f, // y - 5 to make a margin between stamp and bar
                mTextPaint);
    }

    void draw(Canvas canvas) {
        log("Draw:\n"
                + "phantomYMin=" + mPhantomYMin
                + ",\nphantomYMax=" + mPhantomYMax
                + ",\ntargetYMin=" + mTargetYMin
                + ",\ntargetYMax=" + mTargetYMax);

        int startXCoordinate = mView.getPaddingLeft();
        int stopXCoordinate = mView.getMeasuredWidth() - mView.getPaddingRight();

        int fadeInAlpha = (int) (255 * mAlpha);
        int fadeOutAlpha = (int) (255 * (1 - mAlpha));

        final float lineStrokeWidth = mLinePaint.getStrokeWidth();

        if (mIsAnimating) {
            // Here, we're drawing phantom lines

            float phantomLineStartValue = mPhantomYMin;
            float phantomLineStep = (mPhantomYMax - mPhantomYMin) / mLineCount;

            // Drawing lines that are fading out
            mLinePaint.setAlpha(fadeOutAlpha);
            mTextPaint.setAlpha(fadeOutAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = phantomLineStartValue + i * phantomLineStep;

                float y = CommonHelper.findYCoordinate(
                        mView,
                        mPhantomYMin,
                        mPhantomYMax,
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

            float targetLineStartValue = mTargetYMin;
            float targetLineStep = (mTargetYMax - mTargetYMin) / mLineCount;

            // Drawing fading in bars
            mLinePaint.setAlpha(fadeInAlpha);
            mTextPaint.setAlpha(fadeInAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = targetLineStartValue + i * targetLineStep;

                float y = CommonHelper.findYCoordinate(
                        mView,
                        mTargetYMin,
                        mTargetYMax,
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
        if (mTargetYMin != min || mTargetYMax != max) {

            if (mAnim != null) {
                mAnim.cancel();
                mAnim = null;
            }

            mPhantomYMin = mTargetYMin;
            mPhantomYMax = mTargetYMax;

            if (animate) {
                boolean sensitiveAnimation = true;
                float oldRange = (mTargetYMax - mTargetYMin);
                float newRange = max - min;
                long animDuration = sensitiveAnimation ? calcAnimDuration(oldRange, newRange) : Y_AXIS_ANIM_DURATION;

                PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, mTargetYMin, min);
                PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, mTargetYMax, max);
                PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, 0.1f, 1f);

                ObjectAnimator newAnim = ObjectAnimator.ofPropertyValuesHolder(this, h1, h2, h3);
                newAnim.addListener(mAnimListener);
                newAnim.setDuration(animDuration);
                newAnim.setInterpolator(Y_AXIS_INTERPOLATOR);
                newAnim.start();

                mAnim = newAnim;
            } else {
                mAnim = null;
                mTargetYMin = min;
                mTargetYMax = max;
                mAlpha = 1f;
                requestRedraw();
            }
        }
    }

    float getTargetMinValue() {
        return mTargetYMin;
    }

    float getTargetMaxValue() {
        return mTargetYMax;
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
