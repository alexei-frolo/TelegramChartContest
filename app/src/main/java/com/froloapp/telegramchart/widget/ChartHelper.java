package com.froloapp.telegramchart.widget;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Helps to draw line charts;
 * Handles start and stop X positions;
 * Animates appearing or disappearing of chart lines;
 */
final class ChartHelper {

    private static class MinMax {
        float min;
        float max;
    }

    private final AbsChartView mView;

    // other helpers
    private final YAxisHelper mYAxisHelper;
    private final XAxisHelper mXAxisHelper;
    private final List<LineHelper> mLineHelpers = new ArrayList<>();

    private boolean mWillDrawXAxis = true;
    private boolean mWillDrawYAxis = true;

    private List<Point> mPoints = Collections.emptyList();

    private float mStartXPosition = 0f;
    private float mStopXPosition = 0f;

    // Caching local minimums and maximums for optimization
    private final List<Float> mLocalMin = new ArrayList<>();
    private final List<Float> mLocalMax = new ArrayList<>();

    // A try to reuse the same instance for further optimizations
    private final MinMax mMinMax = new MinMax();

    ChartHelper(AbsChartView view) {
        this.mView = view;
        this.mXAxisHelper = new XAxisHelper(view);
        this.mYAxisHelper = new YAxisHelper(view);
    }

    // finds min value for the given index
    private float findMinValueAt(int index) {
        boolean atLeastOneLineVisible = false;
        float min = Float.MAX_VALUE;

        for (LineHelper helper : mLineHelpers) {
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

        for (LineHelper helper : mLineHelpers) {
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
        mYAxisHelper.setMaxAndMin(mMinMax.min, mMinMax.max, animate);
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
        mXAxisHelper.setPoints(points);
        mLineHelpers.clear();
        for (Line line : lines) {
            LineHelper helper = new LineHelper(mView, points, line);
            helper.setXPosition(mStartXPosition, mStopXPosition);
            mLineHelpers.add(helper);
        }
        calcLocalMinAndMAx();
        dispatchMinAndMaxInRange(animate);
    }

    void setXPositions(float startXPosition, float stopXPosition, boolean animate) {
        this.mStartXPosition = startXPosition;
        this.mStopXPosition = stopXPosition;
        mXAxisHelper.setXPositions(startXPosition, stopXPosition, animate);
        for (LineHelper helper : mLineHelpers) {
            helper.setXPosition(startXPosition, stopXPosition);
        }
        dispatchMinAndMaxInRange(animate);
    }

    boolean isLineVisible(Line line) {
        for (LineHelper helper : mLineHelpers) {
            if (helper.getLine().equals(line)) {
                return helper.isVisible();
            }
        }
        return false;
    }

    void show(Line line, boolean animate) {
        setLineVisibility(line, true, animate);
    }

    void hide(Line line, boolean animate) {
        setLineVisibility(line, false, animate);
    }

    private void setLineVisibility(Line targetLine, boolean isVisible, boolean animate) {
        if (isVisible && getVisibleLineCount() == 0) {
            for (LineHelper helper : mLineHelpers) {
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
            for (int i = 0; i < mLineHelpers.size(); i++) {
                LineHelper helper = mLineHelpers.get(i);
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

    private int getVisibleLineCount() {
        int count = 0;
        for (LineHelper helper : mLineHelpers) {
            if (helper.isVisible()) {
                count++;
            }
        }
        return count;
    }

    void draw(Canvas canvas) {
        if (mWillDrawXAxis) {
            mXAxisHelper.draw(canvas);
        }
        if (mWillDrawYAxis) {
            mYAxisHelper.draw(canvas);
        }
        for (LineHelper helper : mLineHelpers) {
            helper.draw(
                    canvas,
                    mYAxisHelper.getCurrentMinValue(),
                    mYAxisHelper.getCurrentMaxValue());
        }
    }

    void attach() {
        mXAxisHelper.attach();
        mYAxisHelper.attach();
        for (LineHelper helper : mLineHelpers) {
            helper.attach();
        }
    }

    void measured() {
        mXAxisHelper.measured();
        mYAxisHelper.measured();
        for (LineHelper helper : mLineHelpers) {
            helper.measured();
        }
    }

    void detach() {
        mXAxisHelper.detach();
        mYAxisHelper.detach();
        for (LineHelper helper : mLineHelpers) {
            helper.detach();
        }
    }
}
