package com.froloapp.chart.widget;

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

    // Finds the nearest timestamp index to the given target X position
    static int getClosestPointIndex(List<Point> points,
                                    float toXPosition) {
        if (points.isEmpty()) {
            return -1;
        }

        long minAxis = points.get(0).stamp;
        long maxAxis = points.get(points.size() - 1).stamp;

        float approximateStamp = (minAxis + ((maxAxis - minAxis) * toXPosition));

        for (int i = 0; i < points.size(); i++) {
            long stamp = points.get(i).stamp;
            if (stamp > approximateStamp) {
                if (i > 0) {
                    float timestampXPosition = ((float) (stamp - minAxis)) / (maxAxis - minAxis);
                    long previousTimestamp = points.get(i - 1).stamp;
                    float previousTimestampXPosition = ((float) (previousTimestamp - minAxis)) / (maxAxis - minAxis);
                    if (Math.abs(previousTimestampXPosition - toXPosition) < Math.abs(timestampXPosition - toXPosition)) {
                        return i - 1;
                    } else {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Invalid timestamp relative position: " + toXPosition);
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
