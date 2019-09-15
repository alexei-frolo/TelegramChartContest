package com.froloapp.telegramchart.widget;

import java.util.List;

final class CommonHelper {
    private CommonHelper() {
    }

    // Finds the nearest timestamp index to the given target X position
    static int findNearestPointIndex(List<Point> points,
                                     float targetXPosition) {
        if (points.isEmpty()) {
            return -1; // early return
        }

        long minStamp = points.get(0).stamp;
        long maxStamp = points.get(points.size() - 1).stamp;
        long approximatelyStamp = minStamp + (long) ((maxStamp - minStamp) * targetXPosition);
        for (int i = 0; i < points.size(); i++) {
            long timestamp = points.get(i).stamp;
            if (timestamp >= approximatelyStamp) {
                return Math.max(0, i - 2); // it's a hack. Must be Math.max(0, i - 1)
            }
        }
        return 0;
    }

    // finds the left closest point to the given target X position
    // then returns its list index.
    static int findVeryLeftPointIndex(List<Point> points,
                                      float targetXPosition) {
        if (points.isEmpty()) {
            return -1; // early return
        }

        long minStamp = points.get(0).stamp;
        long maxStamp = points.get(points.size() - 1).stamp;
        long approximatelyStamp = minStamp + (long) ((maxStamp - minStamp) * targetXPosition);
        for (int i = 0; i < points.size(); i++) {
            long timestamp = points.get(i).stamp;
            if (timestamp >= approximatelyStamp) {
                return Math.max(0, i - 2); // it's a hack. Must be Math.max(0, i - 1)
            }
        }
        return 0;
    }

    static float calcPointRelativePositionAt(List<Point> points,
                                             int index) {
        long stamp = points.get(index).stamp;
        long minStamp = points.get(0).stamp;
        long maxStamp = points.get(points.size() - 1).stamp;
        return ((float) (stamp - minStamp)) / (maxStamp - minStamp);
    }

    static float calcPointCountInRange(List<Point> points,
                                       float startXPosition,
                                       float stopXPosition,
                                       int pointStep) {
        return points.size() * (stopXPosition - startXPosition) / pointStep;
    }

    static float findXCoordinate(AbsChartView view,
                                 float startXPosition,
                                 float stopXPosition,
                                 float targetXPosition) {
        int contentWidth = view.getMeasuredWidth() - view.getPaddingLeft() - view.getPaddingRight();
        float xRelative = ((float) (targetXPosition - startXPosition)) / (stopXPosition - startXPosition);
        return (view.getPaddingLeft() + xRelative * contentWidth);
    }

    static float findYCoordinate(AbsChartView view,
                                 float minValue,
                                 float maxValue,
                                 float targetValue) {
        int contentHeight = view.getMeasuredHeight() - view.getPaddingTop() - view.getPaddingBottom() - view.getFooterHeight();
        float yRelative = (targetValue - minValue) / (maxValue - minValue);
        return (int) (view.getMeasuredHeight() - view.getPaddingTop() - view.getFooterHeight() - yRelative * contentHeight);
    }

    // Calculate relative X position for the given X coordinate
    static float calcCoordinateRelativePosition(AbsChartView view,
                                                float startXPosition,
                                                float stopXPosition,
                                                float xCoordinate) {
        int contentWidth = view.getMeasuredWidth() - view.getPaddingLeft() - view.getPaddingRight();
        float xRelative = ((float) (xCoordinate - view.getPaddingLeft())) / contentWidth;
        return xRelative * (stopXPosition - startXPosition) + startXPosition;
    }
}
