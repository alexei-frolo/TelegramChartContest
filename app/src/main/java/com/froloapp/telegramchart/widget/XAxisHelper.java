package com.froloapp.telegramchart.widget;

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
import android.util.AttributeSet;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.froloapp.telegramchart.R;

import java.util.Collections;
import java.util.List;


// This helper is responsible for animating and drawing X axis
final class XAxisHelper {
    private static final long X_AXIS_ANIM_DURATION = Config.X_AXIS_ANIM_DURATION;

    private static final Interpolator X_AXIS_INTERPOLATOR =
            new AccelerateDecelerateInterpolator();

    private static final float DEFAULT_TEXT_SIZE_IN_SP = 16f;

    private final static Property<XAxisHelper, Float> X_AXIS_ALPHA =
            new Property<XAxisHelper, Float>(float.class, "mAlpha") {
        @Override public Float get(XAxisHelper object) {
            return object.mAlpha;
        }
        @Override public void set(XAxisHelper object, Float value) {
            object.mAlpha = value;
            object.requestRedraw();
        }
    };

    private final AbsChartView mView;
    private List<Point> mPoints = Collections.emptyList();

    private float mStartXPosition = 0;
    private float mStopXPosition = 1f;

    private final float mMaxPointCountInRange = 6f;
    private final float mMinPointCountInRange = 3f;

    // On the X axis, points are drawn by this step:
    // E.i. if the step is 2, then each second point will be drawn.
    // If the step is 5, then each fifth point will be drawn.
    // If the step is 1, then every point will be drawn.
    private int mPointStep = 5;

    // Old step: points are drawn with fade out animation
    private int mPhantomPointStep = mPointStep;

    private float mAlpha = 1f;

    private boolean mIsAnimating = false;

    private ValueAnimator mAnim;

    private final Animator.AnimatorListener mAnimListener =
            new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIsAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsAnimating = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mIsAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            };

    // Paint tools
    //private final Paint mLinePaint;
    private final Paint mTextPaint;

    XAxisHelper(AbsChartView view) {
        this.mView = view;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(Misc.spToPx(DEFAULT_TEXT_SIZE_IN_SP, view.getContext()));
        mTextPaint = textPaint;
    }

    private void requestRedraw() {
        mView.invalidate();
    }

    private void drawPointsWithoutTransition(Canvas canvas) {
        final float y = mView.getMeasuredHeight() - mView.getPaddingBottom();

        final int pointCount = mPoints.size();
        final float averageXPositionStep = 1f / pointCount; // average step

        int pointIndex = CommonHelper.findVeryLeftPointIndex(mPoints, mStartXPosition);

        if (pointIndex == -1) {
            return;
        }

        pointIndex = (pointIndex / mPointStep) * mPointStep; // normalizing
        float pointXPosition = CommonHelper.calcPointRelativePositionAt(mPoints, pointIndex);

        mTextPaint.setAlpha(255);

        while (pointIndex < pointCount) {
            String text = mPoints.get(pointIndex).text;

            float x = CommonHelper.findXCoordinate(
                    mView,
                    mStartXPosition,
                    mStopXPosition,
                    pointXPosition);

            canvas.drawText(text, x, y, mTextPaint);
            if (pointXPosition > mStopXPosition) {
                break;
            }
            pointIndex += mPointStep;
            pointXPosition += mPointStep * averageXPositionStep;
        }
    }

    private void drawPointsWithTransition(Canvas canvas) {
        final float y = mView.getMeasuredHeight() - mView.getPaddingBottom();

        final int pointCount = mPoints.size();
        final float averageXPositionStep = 1f / mPoints.size(); // average step on x axis

        // if true, then the fade in animation will be used, otherwise - fade out animation
        final boolean fadeIn = mPointStep < mPhantomPointStep;
        final int smallStep = fadeIn ? mPointStep : mPhantomPointStep;
        final int bigStep = fadeIn ? mPhantomPointStep : mPointStep;

        int pointIndex = CommonHelper.findVeryLeftPointIndex(mPoints, mStartXPosition);

        if (pointIndex == -1) {
            return;
        }

        pointIndex = (pointIndex / smallStep) * smallStep; // normalize

        float pointXPosition = CommonHelper.calcPointRelativePositionAt(mPoints, pointIndex);

        final int alpha = fadeIn ? (int) (mAlpha * 255) : (int) ((1 - mAlpha) * 255);

        while (pointIndex < pointCount) {
            String text = mPoints.get(pointIndex).text;
            float x = CommonHelper.findXCoordinate(
                    mView,
                    mStartXPosition,
                    mStopXPosition,
                    pointXPosition);

            if (pointIndex % bigStep != 0) {
                // means that's phantom
                mTextPaint.setAlpha(alpha);
            } else {
                mTextPaint.setAlpha(255);
            }

            canvas.drawText(text, x, y, mTextPaint);

            if (pointXPosition > mStopXPosition) {
                break;
            }
            pointIndex += smallStep;
            pointXPosition += smallStep * averageXPositionStep;
        }
    }

    private void checkPointStepChanged(boolean animateChanges) {
        boolean changed = false;
        if (mPointStep < 1) {
            mPointStep = 1;
            changed = true;
        }

        float currentPointCountInRange = CommonHelper.calcPointCountInRange(
                mPoints,
                mStartXPosition,
                mStopXPosition,
                mPointStep);

        if (currentPointCountInRange > mMaxPointCountInRange) {
            mPhantomPointStep = mPointStep > 0 ? mPointStep : 1;
            while (currentPointCountInRange > mMaxPointCountInRange) {
                mPointStep *= 2;
                currentPointCountInRange = CommonHelper.calcPointCountInRange(
                        mPoints,
                        mStartXPosition,
                        mStopXPosition,
                        mPointStep);
            }
            changed = true;
        } else if (currentPointCountInRange < mMinPointCountInRange) {
            while (currentPointCountInRange < mMinPointCountInRange) {
                mPhantomPointStep = mPointStep > 0 ? mPointStep : 1;
                mPointStep /= 2;
                if (mPointStep < 1) {
                    mPointStep = 1;
                    break;
                }
                currentPointCountInRange = CommonHelper.calcPointCountInRange(
                        mPoints,
                        mStartXPosition,
                        mStopXPosition,
                        mPointStep);
            }
            changed = true;
        }

        if (changed) {
            if (mAnim != null) {
                mAnim.cancel();
                mAnim = null;
            }

            if (animateChanges) {
                PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(X_AXIS_ALPHA, 0.1f, 1f);
                ObjectAnimator newAnim = ObjectAnimator.ofPropertyValuesHolder(this, holder);
                newAnim.setDuration(X_AXIS_ANIM_DURATION);
                newAnim.setInterpolator(X_AXIS_INTERPOLATOR);
                newAnim.addListener(mAnimListener);
                newAnim.start();

                mAnim = newAnim;
            } else {
                mAlpha = 1f;
                requestRedraw();
            }
        }
    }

    void loadAttributes(Context context, AttributeSet attrs) {
        final int xAxisColor;
        final int xAxisTextColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.AbsChartView, 0, 0);
            xAxisColor = typedArray.getColor(R.styleable.AbsChartView_xAxisColor,
                    Color.GRAY);
            xAxisTextColor = typedArray.getColor(R.styleable.AbsChartView_xAxisTextColor,
                    Color.GRAY);
            typedArray.recycle();
        } else {
            xAxisColor = Color.GRAY;
            xAxisTextColor = Color.GRAY;
        }

        mTextPaint.setColor(xAxisTextColor);
    }

    void setPoints(List<Point> points) {
        this.mPoints = points;
        checkPointStepChanged(false);
    }

    void setXPositions(float start, float stop, boolean animate) {
        mStartXPosition = start;
        mStopXPosition = stop;
        checkPointStepChanged(animate);
    }

    void draw(Canvas canvas) {
        if (mIsAnimating) {
            drawPointsWithTransition(canvas);
        } else {
            drawPointsWithoutTransition(canvas);
        }
    }

    void attach() {
    }

    void measured() {
        final String test = "Mar. 1";
        final Rect buffTextBounds = new Rect(); // here we store bounds for stamp text
        mTextPaint.getTextBounds(test, 0, test.length(), buffTextBounds);
        int footerHeight = buffTextBounds.height() + 5; // + 5 to make a margin between stamps and the first y bar
        mView.setFooterHeight(footerHeight);
    }

    void detach() {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
    }

}
