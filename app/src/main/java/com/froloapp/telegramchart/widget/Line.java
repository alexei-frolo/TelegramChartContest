package com.froloapp.telegramchart.widget;


public final class Line {
    private float[] mValues;
    private String name;
    private int color;

    Line(float[] values, String name, int color) {
        this.mValues = values;
        this.name = name;
        this.color = color;
    }

    int getValueCount() {
        return mValues.length;
    }

    float getValueAt(int index) {
        return mValues[index];
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
