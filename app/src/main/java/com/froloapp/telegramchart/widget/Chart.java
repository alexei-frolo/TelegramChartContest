package com.froloapp.telegramchart.widget;

import java.util.ArrayList;
import java.util.List;


public final class Chart {

    public static class Builder {
        private String mName;
        private List<Point> mPoints;
        private List<Line> mLines = new ArrayList<>();

        public Builder(String name) {
            this.mName = name != null ? name : "";
        }

        public Builder addPoints(List<Point> points) {
            this.mPoints = points;
            this.mLines.clear();
            return this;
        }

        public Builder addLine(float[] value, String name, int color) {
            if (mPoints == null) {
                throw new IllegalArgumentException("No points added yet");
            }
            if (mPoints.size() != value.length) {
                throw new IllegalArgumentException("Value count doesn't match point count");
            }
            mLines.add(new Line(value, name, color));
            return this;
        }

        public Chart build() {
            return new Chart(mName, mPoints, mLines);
        }
    }

    private String mChartName;
    private List<Point> mPoints;
    private List<Line> mLines;

    private Chart(String name, List<Point> points, List<Line> lines) {
        this.mChartName = name;
        this.mPoints = points;
        this.mLines = lines;
    }

    List<Point> getPoints() {
        return mPoints;
    }

    List<Line> getLines() {
        return mLines;
    }

    public String getChartName() {
        return mChartName;
    }

    public int getLineCount() {
        return mLines.size();
    }

    public Line getLineAt(int index) {
        return mLines.get(index);
    }

    @Override
    public String toString() {
        return mChartName;
    }
}
