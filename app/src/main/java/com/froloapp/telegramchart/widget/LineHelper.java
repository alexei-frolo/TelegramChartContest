package com.froloapp.telegramchart.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.view.animation.Interpolator;

import java.util.List;


// This helper is responsible for animating and drawing one chart line
final class LineHelper {
    private static final long FADE_ANIM_DURATION = Config.Y_AXIS_ANIM_DURATION;

    private static final Interpolator FADE_INTERPOLATOR =
            new FastOutLinearInInterpolator();

    private static final float DEFAULT_LINE_STROKE_WIDTH_IN_DP = 1.5f;

    private final AbsChartView mView;
    private final List<Point> mPoints;
    private final Line mLine;

    private float mStartXPercentage = 0f;
    private float mStopXPercentage = 1f;

    private float mAlpha = 1f;
    private boolean mIsVisible = true;
    private boolean mIsAnimating = false;

    private ValueAnimator mFadeAnimator;

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

    private final ValueAnimator.AnimatorUpdateListener mUpdater =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator anim) {
                    mAlpha = (float) anim.getAnimatedValue();
                    requestRedraw();
                }
            };

    // Paint tools
    private final Paint mPaint;
    // buffering line (collecting (x; y) coors of a chart line to draw)
    private float[] mBufferLinePoints;

    LineHelper(AbsChartView view, List<Point> points, Line line) {
        this.mView = view;
        this.mPoints = points;
        this.mLine = line;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dpToPx(DEFAULT_LINE_STROKE_WIDTH_IN_DP, view.getContext()));
        mPaint = paint;

        mBufferLinePoints = new float[points.size() * 4];
    }

    private void requestRedraw() {
        mView.invalidate();
    }

    Line getLine() {
        return mLine;
    }

    boolean isVisible() {
        return mIsVisible;
    }

    void setXPosition(float startXPosition, float stopXPosition) {
        this.mStartXPercentage = startXPosition;
        this.mStopXPercentage = stopXPosition;
        requestRedraw();
    }

    void show(boolean animate) {
        mIsVisible = true;

        if (mFadeAnimator != null) {
            mFadeAnimator.cancel();
            mFadeAnimator = null;
        }

        if (animate) {
            float startAlphaValue = mAlpha;
            ValueAnimator newAnim = ValueAnimator.ofFloat(startAlphaValue, 1f);
            newAnim.addListener(mAnimListener);
            newAnim.addUpdateListener(mUpdater);
            newAnim.setDuration(FADE_ANIM_DURATION);
            newAnim.setInterpolator(FADE_INTERPOLATOR);
            newAnim.start();

            mFadeAnimator = newAnim;
        } else {
            requestRedraw();
        }
    }

    void hide(boolean animate) {
        mIsVisible = false;

        if (mFadeAnimator != null) {
            mFadeAnimator.cancel();
            mFadeAnimator = null;
        }

        if (animate) {
            float startAlphaValue = mAlpha;
            ValueAnimator newAnim = ValueAnimator.ofFloat(startAlphaValue, 0f);
            newAnim.addListener(mAnimListener);
            newAnim.addUpdateListener(mUpdater);
            newAnim.setDuration(FADE_ANIM_DURATION);
            newAnim.setInterpolator(FADE_INTERPOLATOR);
            newAnim.start();

            mFadeAnimator = newAnim;
        } else {
            requestRedraw();
        }
    }

    void draw(Canvas canvas, float minValue, float maxValue) {
        // don't draw the line if it's nor visible neither animating
        if (!mIsVisible && !mIsAnimating) {
            return;
        }

        final int pointCount = mPoints.size();
        final int startPointIndex = CommonHelper.findVeryLeftPointIndex(
                mPoints,
                mStartXPercentage);

        if (startPointIndex == -1) {
            return;
        }

        final float startPointXPosition = CommonHelper.calcPointRelativePositionAt(
                mPoints,
                startPointIndex);

        // drawing
        int pointIndex = startPointIndex;
        float pointXPosition = startPointXPosition;
        boolean outsideBounds = false; // make it true when the render-loop must break

        float value = mLine.getValueAt(pointIndex);

        // preparing paint tool
        mPaint.setColor(mLine.color);

        if (mIsAnimating) {
            mPaint.setAlpha((int) (mAlpha * 255));
        } else {
            mPaint.setAlpha(255);
        }

        mPaint.setStyle(Paint.Style.STROKE);

        float xCoor = CommonHelper.findXCoordinate(
                mView,
                mStartXPercentage,
                mStopXPercentage,
                pointXPosition);
        float yCoor = CommonHelper.findYCoordinate(
                mView,
                minValue,
                maxValue,
                value);

        int k = 0; // just a counter
        mBufferLinePoints[k++] = xCoor;
        mBufferLinePoints[k++] = yCoor;

        while (pointIndex < pointCount - 1) {
            pointIndex++;
            // it could be optimized
            pointXPosition = CommonHelper.calcPointRelativePositionAt(
                    mPoints,
                    pointIndex);

            value = mLine.getValueAt(pointIndex);

            xCoor = CommonHelper.findXCoordinate(
                    mView,
                    mStartXPercentage,
                    mStopXPercentage,
                    pointXPosition);
            yCoor = CommonHelper.findYCoordinate(
                    mView,
                    minValue,
                    maxValue,
                    value);

            mBufferLinePoints[k++] = xCoor;
            mBufferLinePoints[k++] = yCoor;

            if (outsideBounds) {
                break;
            }

            if (pointXPosition > mStopXPercentage) {
                // It's enough. No need to draw lines after next point as they will be invisible.
                // So allow to draw one part more and exit;
                outsideBounds = true;
            }

            mBufferLinePoints[k] = mBufferLinePoints[k++ - 2];
            mBufferLinePoints[k] = mBufferLinePoints[k++ - 2];
        }

        canvas.drawLines(mBufferLinePoints, 0, k - 1, mPaint);
    }

    void attach() {
    }

    void measured() {
    }

    void detach() {
        if (mFadeAnimator != null) {
            mFadeAnimator.cancel();
            mFadeAnimator = null;
        }
    }
}