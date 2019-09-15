package com.froloapp.telegramchart.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.froloapp.telegramchart.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Helps to draw line charts;
 * Handles start and stop X positions;
 * Animates appearing or disappearing of chart lines;
 */
final class ChartDelegate {

    private static final float DEFAULT_LINE_STROKE_WIDTH_IN_DP = 1.0f;
    private static final float DEFAULT_SELECTED_LINE_DOT_RADIUS_IN_DP = 4.0f;

    private static class MinMax {
        float min;
        float max;
    }

    private final AbsChartView mView;

    // Delegate helpers
    private final YAxisDelegate mYAxisDelegate;
    private final XAxisDelegate mXAxisDelegate;
    private final List<LineDelegate> mLineDelegates = new ArrayList<>();

    private boolean mWillDrawXAxis = true;
    private boolean mWillDrawYAxis = true;

    private List<Point> mPoints = Collections.emptyList();

    private float mStartXPosition = 0f;
    private float mStopXPosition = 1f;

    // Special vertical line is drawn at this X position
    // If it is in range 0..1
    private float mSelectedXPosition = -1f;

    // Caching local minimums and maximums for optimization
    private final List<Float> mLocalMin = new ArrayList<>();
    private final List<Float> mLocalMax = new ArrayList<>();

    // A try to reuse the same instance for further optimizations
    private final MinMax mMinMax = new MinMax();

    // Paint tools
    private final Paint mPaint;
    private final Paint mDotPaint;
    private final float mSelectedXPositionDotRadius;

    ChartDelegate(AbsChartView view) {
        this.mView = view;
        this.mXAxisDelegate = new XAxisDelegate(view);
        this.mYAxisDelegate = new YAxisDelegate(view);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(Misc.dpToPx(DEFAULT_LINE_STROKE_WIDTH_IN_DP, view.getContext()));
        mPaint = paint;

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(Misc.dpToPx(DEFAULT_LINE_STROKE_WIDTH_IN_DP, view.getContext()));
        mDotPaint = dotPaint;

        mSelectedXPositionDotRadius = Misc.dpToPx(
                DEFAULT_SELECTED_LINE_DOT_RADIUS_IN_DP,
                view.getContext());
    }

    // finds min value for the given index
    private float findMinValueAt(int index) {
        boolean atLeastOneLineVisible = false;
        float min = Float.MAX_VALUE;

        for (LineDelegate helper : mLineDelegates) {
            if (!helper.isVisible()) {
                // the line is invisible => skip it
                continue;
            }

            atLeastOneLineVisible = true;

            Line line = helper.getLine();
            float value = line.getValueAt(index);
            if (value < min) {
                min = value;
            }
        }
        if (atLeastOneLineVisible) {
            return min;
        } else return 0f; // by default min is 0
    }

    // finds max value for the given index
    private float findMaxValueAt(int index) {
        boolean atLeastOneLineVisible = false;
        float max = Float.MIN_VALUE;

        for (LineDelegate helper : mLineDelegates) {
            if (!helper.isVisible()) {
                // the line is invisible => skip it
                continue;
            }

            atLeastOneLineVisible = true;

            Line line = helper.getLine();
            float value = line.getValueAt(index);
            if (value > max) {
                max = value;
            }
        }
        if (atLeastOneLineVisible) {
            return max;
        } else return 10f; // by default max is 10
    }

    private float getMinValueAt(int index) {
        return mLocalMin.get(index);
    }

    private float getMaxValueAt(int index) {
        return mLocalMax.get(index);
    }

    private void calcLocalMinAndMAx() {
        mLocalMin.clear();
        mLocalMax.clear();
        for (int i = 0; i < mPoints.size(); i++) {
            float minValue = findMinValueAt(i);
            float maxValue = findMaxValueAt(i);
            mLocalMin.add(minValue);
            mLocalMax.add(maxValue);
        }
    }

    private void findLocalMinMax(MinMax holder, float fromXPosition, float toXPosition) {
        if (mPoints.isEmpty()) {
            return;
        }

        long startStamp = mPoints.get(0).stamp;
        long stopStamp = mPoints.get(mPoints.size() - 1).stamp;

        long fromStamp = (long) (startStamp + (stopStamp - startStamp) * fromXPosition) - 1;
        long toStamp = (long) (startStamp + (stopStamp - startStamp) * toXPosition) + 1;

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int i = 0; i < mPoints.size(); i++) {
            long stamp = mPoints.get(i).stamp;
            if (stamp < fromStamp) {
                if (i < mPoints.size() - 1) {
                    // check if the next axis is in the bounds
                    long nextStamp = mPoints.get(i + 1).stamp;
                    if (nextStamp >= fromStamp) {
                        float localMin = getMinValueAt(i);
                        if (localMin < min) {
                            min = localMin;
                        }
                        float localMax = getMaxValueAt(i);
                        if (localMax > max) {
                            max = localMax;
                        }
                    }
                }
                continue;
            }
            if (stamp > toStamp) {
                float localMin = getMinValueAt(i);
                if (localMin < min) {
                    min = localMin;
                }
                float localMax = getMaxValueAt(i);
                if (localMax > max) {
                    max = localMax;
                }
                break;
            }
            float localMin = getMinValueAt(i);
            if (localMin < min) {
                min = localMin;
            }
            float localMax = getMaxValueAt(i);
            if (localMax > max) {
                max = localMax;
            }
        }
        holder.min = min;
        holder.max = max;
    }

    private void dispatchMinAndMaxInRange(boolean animate) {
        findLocalMinMax(mMinMax, mStartXPosition, mStopXPosition);
        mYAxisDelegate.setMaxAndMin(mMinMax.min, mMinMax.max, animate);
    }

    void loadAttributes(Context context, AttributeSet attrs) {
        final int xSelectedStampLineColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.ChartView, 0, 0);
            xSelectedStampLineColor = typedArray.getColor(R.styleable.ChartView_clickedStampLineColor,
                    Color.GRAY);
            typedArray.recycle();
        } else {
            xSelectedStampLineColor = Color.GRAY;
        }
        mPaint.setColor(xSelectedStampLineColor);

        mXAxisDelegate.loadAttributes(context, attrs);
        mYAxisDelegate.loadAttributes(context, attrs);
    }

    boolean willDrawXAxis() {
        return mWillDrawXAxis;
    }

    boolean willDrawYAxis() {
        return mWillDrawYAxis;
    }

    void setWillDrawXAxis(boolean willDraw) {
        mWillDrawXAxis = willDraw;
        mView.requestLayout();
    }

    void setWillDrawYAxis(boolean willDraw) {
        mWillDrawYAxis = willDraw;
        mView.requestLayout();
    }

    void setChart(List<Point> points, List<Line> lines, boolean animate) {
        mPoints = points;
        mXAxisDelegate.setPoints(points);
        mLineDelegates.clear();
        for (Line line : lines) {
            LineDelegate helper = new LineDelegate(mView, points, line);
            helper.setXPosition(mStartXPosition, mStopXPosition);
            mLineDelegates.add(helper);
        }
        calcLocalMinAndMAx();
        dispatchMinAndMaxInRange(animate);
    }

    List<Point> getPoints() {
        return mPoints;
    }

    float getStartXPosition() {
        return mStartXPosition;
    }

    float getStopXPosition() {
        return mStopXPosition;
    }

    void setXPositions(float startXPosition, float stopXPosition, boolean animate) {
        this.mStartXPosition = startXPosition;
        this.mStopXPosition = stopXPosition;
        mXAxisDelegate.setXPositions(startXPosition, stopXPosition, animate);
        for (LineDelegate helper : mLineDelegates) {
            helper.setXPosition(startXPosition, stopXPosition);
        }
        dispatchMinAndMaxInRange(animate);
    }

    void setSelectedXPosition(float targetXPosition) {
        this.mSelectedXPosition = targetXPosition;
        mView.invalidate();
    }

    void clearSelectedXPosition() {
        this.mSelectedXPosition = -1f;
        mView.invalidate();
    }

    int getLineCount() {
        return mLineDelegates.size();
    }

    Line getLineAt(int index) {
        return mLineDelegates.get(index).getLine();
    }

    boolean isLineVisible(Line line) {
        for (LineDelegate helper : mLineDelegates) {
            if (helper.getLine().equals(line)) {
                return helper.isVisible();
            }
        }
        return false;
    }

    int getVisibleLineCount() {
        int count = 0;
        for (LineDelegate helper : mLineDelegates) {
            if (helper.isVisible()) {
                count++;
            }
        }
        return count;
    }

    void show(Line line, boolean animate) {
        setLineVisibility(line, true, animate);
    }

    void hide(Line line, boolean animate) {
        setLineVisibility(line, false, animate);
    }

    private void setLineVisibility(Line targetLine, boolean isVisible, boolean animate) {
        if (isVisible && getVisibleLineCount() == 0) {
            for (LineDelegate helper : mLineDelegates) {
                Line line = helper.getLine();
                if (line.equals(targetLine)) {
                    helper.show(animate);
                }
            }
            // Full recalculation
            calcLocalMinAndMAx();
            dispatchMinAndMaxInRange(animate);
        } else {
            // optimized way (recalculation just for local timestamps if needed)
            for (int i = 0; i < mLineDelegates.size(); i++) {
                LineDelegate helper = mLineDelegates.get(i);
                Line line = helper.getLine();
                if (line.equals(targetLine)) {

                    if (isVisible) {
                        helper.show(animate);
                    } else {
                        helper.hide(animate);
                    }

                    for (int j = 0; j < mPoints.size(); j++) {

                        float value = line.getValueAt(j);

                        float currMinValue = mLocalMin.get(j);
                        float currMaxValue = mLocalMax.get(j);

                        if (isVisible) {
                            if (value < currMinValue) {
                                mLocalMin.set(j, value);
                            }
                            if (value > currMaxValue) {
                                mLocalMax.set(j, value);
                            }
                        } else {
                            if (value <= currMinValue) {
                                float newMinValue = findMinValueAt(j);
                                mLocalMin.set(j, newMinValue);
                            }
                            if (value >= currMaxValue) {
                                float newMaxValue = findMaxValueAt(j);
                                mLocalMax.set(j, newMaxValue);
                            }
                        }
                    }
                    break;
                }
            }
            dispatchMinAndMaxInRange(animate);
        }
    }

    private void drawSelectedXPositionLine(Canvas canvas) {
        if (mSelectedXPosition >= 0.0f
                && mSelectedXPosition <= 1.0f) {

            // At first, normalizing X position

            int index = CommonHelper.getClosestPointIndex(
                    mPoints,
                    mSelectedXPosition);

            float correctXPosition = CommonHelper.calcPointRelativePositionAt(
                    mPoints,
                    index);

            float xCoordinate = CommonHelper.findXCoordinate(
                    mView,
                    mStartXPosition,
                    mStopXPosition,
                    correctXPosition);

            float yTop = mView.getPaddingTop();
            float yBottom = mView.getMeasuredHeight() - mView.getPaddingBottom() - mView.getFooterHeight();

            canvas.drawLine(
                    xCoordinate,
                    yTop,
                    xCoordinate,
                    yBottom,
                    mPaint);
        }
    }

    private void drawSelectedXPositionDots(Canvas canvas) {
        if (mSelectedXPosition >= 0.0f
                && mSelectedXPosition <= 1.0f) {

            // At first, normalizing X position

            int index = CommonHelper.getClosestPointIndex(
                    mPoints,
                    mSelectedXPosition);

            float correctXPosition = CommonHelper.calcPointRelativePositionAt(
                    mPoints,
                    index);

            float xCoordinate = CommonHelper.findXCoordinate(
                    mView,
                    mStartXPosition,
                    mStopXPosition,
                    correctXPosition);

            for (LineDelegate helper : mLineDelegates) {
                if (helper.isVisible()) {

                    Line line = helper.getLine();

                    // draw only if it's visible
                    float yCoordinate = CommonHelper.findYCoordinate(
                            mView,
                            mYAxisDelegate.getCurrentMinValue(),
                            mYAxisDelegate.getCurrentMaxValue(),
                            line.getValueAt(index));

                    mDotPaint.setColor(line.getColor());

                    canvas.drawCircle(
                            xCoordinate,
                            yCoordinate,
                            mSelectedXPositionDotRadius,
                            mDotPaint);
                }
            }
        }
    }

    void draw(Canvas canvas) {
        // draw selected X position line at first
        drawSelectedXPositionLine(canvas);

        if (mWillDrawXAxis) {
            mXAxisDelegate.draw(canvas);
        }
        if (mWillDrawYAxis) {
            mYAxisDelegate.draw(canvas);
        }
        for (LineDelegate helper : mLineDelegates) {
            helper.draw(
                    canvas,
                    mYAxisDelegate.getCurrentMinValue(),
                    mYAxisDelegate.getCurrentMaxValue());
        }

        // draw selected X position dots at last
        drawSelectedXPositionDots(canvas);
    }

    void attach() {
        mXAxisDelegate.attach();
        mYAxisDelegate.attach();
        for (LineDelegate helper : mLineDelegates) {
            helper.attach();
        }
    }

    void measured() {
        mXAxisDelegate.measured();
        mYAxisDelegate.measured();
        for (LineDelegate helper : mLineDelegates) {
            helper.measured();
        }
    }

    void detach() {
        mXAxisDelegate.detach();
        mYAxisDelegate.detach();
        for (LineDelegate helper : mLineDelegates) {
            helper.detach();
        }
    }
}
