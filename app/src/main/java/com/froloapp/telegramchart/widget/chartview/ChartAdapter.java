package com.froloapp.telegramchart.widget.chartview;


public interface ChartAdapter {
    long getMinXAxis();
    long getMaxXAxis();

    boolean hasNextAxis(long afterXAxis);
    long getNextAxis(long afterXAxis);

    int getMinValue(float fromXAxisRel, float toXAxisRel);
    int getMaxValue(float fromXAxisRel, float toXAxisRel);

    boolean hasAxisAfter(float timestampRel);
    long getNextAxis(float timestampRel);
    float getNextAxisRel(float timestampRel);

    int getChartCount();
    ChartData getChart(int index);
}
