package com.froloapp.telegramchart.widget;


import java.util.List;

public class Chart {

    // Do we need some builder?

    public static Chart create(List<Point> points, List<Line> lines) {
        return new Chart(points, lines);
    }

    private final List<Point> mPoints;
    private final List<Line> mLines;

    private Chart(List<Point> points, List<Line> lines) {
        this.mPoints = points;
        this.mLines = lines;
    }

    List<Point> getPoints() {
        return mPoints;
    }

    List<Line> getLines() {
        return mLines;
    }

    public int getLineCount() {
        return mLines.size();
    }

    public Line getLineAt(int index) {
        return mLines.get(index);
    }
}
