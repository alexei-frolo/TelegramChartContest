package com.froloapp.telegramchart.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.BuildConfig;


// This helper is responsible for animating and drawing Y axis
final class YAxisHelper {

    private static final String LOG_TAG = "YAxisHelper";

    private static final long Y_AXIS_ANIM_DURATION = Config.Y_AXIS_ANIM_DURATION;

    private static final Interpolator Y_AXIS_INTERPOLATOR =
            new AccelerateDecelerateInterpolator();

    private static final float DEFAULT_LINE_STROKE_WIDTH_IN_DP = 2f;
    private static final float DEFAULT_TEXT_SIZE_IN_SP = 16f;

    private final static Property<YAxisHelper, Float> MIN_Y_VALUE =
            new Property<YAxisHelper, Float>(float.class, "mCurrentYMin") {
        @Override public Float get(YAxisHelper object) {
            return object.mCurrentYMin;
        }
        @Override public void set(YAxisHelper object, Float value) {
            object.mCurrentYMin = value;
            object.requestRedraw();
        }
    };

    private final static Property<YAxisHelper, Float> MAX_Y_VALUE =
            new Property<YAxisHelper, Float>(float.class, "mCurrentYMax") {
        @Override public Float get(YAxisHelper object) {
            return object.mCurrentYMax;
        }
        @Override public void set(YAxisHelper object, Float value) {
            object.mCurrentYMax = value;
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

    // count of horizontal lines
    private final int mLineCount = 5;

    // target Y min and max
    private float mTargetYMin;
    private float mTargetYMax;
    // target value step between lines
    private float mTargetYStep;

    // current Y min and max
    private float mCurrentYMin;
    private float mCurrentYMax;
    // current value step between lines
    private float mCurrentYStep;

    // old Y min and max
    private float mPhantomYMin = 0;
    private float mPhantomYMax = 0;
    // old value step between lines
    private float mPhantomYStep = 0;

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
        String text = Utils.format((int) value);
        canvas.drawText(
                text,
                startXCoordinate,
                yCoordinate - mLinePaint.getStrokeWidth() * 4, // mLinePaint.getStrokeWidth() * 4 adds additional space between line and text
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
            mPhantomYStep = (mPhantomYMax - mPhantomYMin) / mLineCount;

            // Drawing lines that are fading out
            mLinePaint.setAlpha(fadeOutAlpha);
            mTextPaint.setAlpha(fadeOutAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = mPhantomYMin + i * mPhantomYStep;

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

            mCurrentYStep = (mCurrentYMax - mCurrentYStep) / mLineCount;

            // Drawing fading in bars
            mLinePaint.setAlpha(fadeInAlpha);
            mTextPaint.setAlpha(fadeInAlpha);

            for (int i = 0; i < mLineCount; i++) {
                float value = mCurrentYMin + i * mCurrentYStep;

                float y = CommonHelper.findYCoordinate(
                        mView,
                        mCurrentYMin,
                        mCurrentYMax,
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

            float oldRange = (mCurrentYMax - mCurrentYMin);
            float newRange = max - min;

            mTargetYMin = min;
            mTargetYMax = max;
            mTargetYStep = newRange / (mLineCount);

            final boolean wasAnimating;

            final float startAlpha;

            if (mAnim != null) {
                wasAnimating = mAnim.isRunning();
                mAnim.cancel();
                mAnim = null;
            } else {
                wasAnimating = false;
            }

            mCurrentYStep = (mCurrentYMax - mCurrentYStep) / mLineCount;

            if (!wasAnimating) {
                startAlpha = 0.1f;
                mPhantomYMin = mCurrentYMin;
                mPhantomYMax = mCurrentYMax;
                mPhantomYStep = mCurrentYStep;
            } else {
                startAlpha = mAlpha;
                // phantom values don't change if it was not animating
            }


            if (animate) {
                boolean sensitiveAnimation = false;
                long animDuration = sensitiveAnimation ? calcAnimDuration(oldRange, newRange) : Y_AXIS_ANIM_DURATION;

                PropertyValuesHolder h1 = PropertyValuesHolder.ofFloat(MIN_Y_VALUE, mCurrentYMin, min);
                PropertyValuesHolder h2 = PropertyValuesHolder.ofFloat(MAX_Y_VALUE, mCurrentYMax, max);
                PropertyValuesHolder h3 = PropertyValuesHolder.ofFloat(Y_AXIS_ALPHA, startAlpha, 1f);

                ObjectAnimator newAnim = ObjectAnimator.ofPropertyValuesHolder(this, h1, h2, h3);
                newAnim.addListener(mAnimListener);
                newAnim.setDuration(animDuration);
                newAnim.setInterpolator(Y_AXIS_INTERPOLATOR);
                newAnim.start();

                mAnim = newAnim;
            } else {
                mAnim = null;
                mCurrentYMin = min;
                mCurrentYMax = max;
                mAlpha = 1f;
                requestRedraw();
            }
        }
    }

    float getCurrentMinValue() {
        return mCurrentYMin;
    }

    float getCurrentMaxValue() {
        return mCurrentYMax;
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
