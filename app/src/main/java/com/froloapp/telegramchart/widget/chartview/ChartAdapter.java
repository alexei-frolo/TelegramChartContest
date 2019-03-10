package com.froloapp.telegramchart.widget.chartview;


public interface ChartAdapter {
    long getMinXAxis();
    long getMaxXAxis();

    int getMinValue(long fromXAxis, long toXAxis);
    int getMaxValue(long fromXAxis, long toXAxis);

    boolean hasNextAxis(long afterXAxis);
    long getNextAxis(long afterXAxis);

    int getChartCount();
    ChartData getChart(int index);
}
