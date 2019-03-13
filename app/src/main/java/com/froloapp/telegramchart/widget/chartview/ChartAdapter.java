package com.froloapp.telegramchart.widget.chartview;


public interface ChartAdapter {
    int getChartCount();
    ChartData getChart(int index);
    boolean isVisible(ChartData chart);
    void setVisible(ChartData chart, boolean visible);

    long getMinTimestamp();
    long getMaxTimestamp();

    long[] getTimestamps(float fromXPosition, float toXPosition);

    boolean hasPreviousTimestamp(float beforeTimestampPosition);
    long getPreviousTimestamp(float beforeTimestampPosition);
    float getPreviousTimestampPosition(float beforeTimestampPosition);

    boolean hasNextTimestamp(float afterTimestampPosition);
    long getNextTimestamp(float afterTimestampPosition);
    float getNextTimestampPosition(float afterTimestampPosition);

    boolean hasNextTimestamp(long afterTimestamp);
    long getNextTimestamp(long afterTimestamp);

    int getMinYValue(float fromTimestampPosition, float toTimestampPosition);
    int getMaxXValue(float fromTimestampPosition, float toTimestampPosition);
}
