package com.froloapp.telegramchart.widget.chartview;


public interface ChartAdapter {
    int getChartCount();
    ChartData getChart(int index);

    boolean hasNextTimestamp(float afterTimestampPosition);
    long getNextTimestamp(float afterTimestampPosition);
    float getNextTimestampPosition(float afterTimestampPosition);

    boolean hasNextTimestamp(long afterTimestamp);
    long getNextTimestamp(long afterTimestamp);

    int getMinYValue(float fromTimestampPosition, float toTimestampPosition);
    int getMaxXValue(float fromTimestampPosition, float toTimestampPosition);
}
