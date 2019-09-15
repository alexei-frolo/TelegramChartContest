package com.froloapp.telegramchart.widget;

public final class Point {

    public static Point create(long stamp, String text) {
        return new Point(stamp, text);
    }

    final long stamp;
    final String text;

    private Point(long stamp, String text) {
        this.stamp = stamp;
        this.text = text;
    }
}
