package com.froloapp.telegramchart.widget.chartview;


// Should be as effective as possible
public interface ChartAdapter {
    int getChartCount();
    ChartData getChart(int index);
    boolean isVisible(ChartData chart);
    void setVisible(ChartData chart, boolean visible);

    float getClosestTimestampPosition(float toXPosition);
    long getClosestTimestamp(float toXPosition);

    boolean hasPreviousTimestamp(float beforeTimestampPosition);
    long getPreviousTimestamp(float beforeTimestampPosition);
    float getPreviousTimestampPosition(float beforeTimestampPosition);

    boolean hasNextTimestamp(float afterTimestampPosition);
    long getNextTimestamp(float afterTimestampPosition);
    float getNextTimestampPosition(float afterTimestampPosition);

    boolean hasNextTimestamp(long afterTimestamp);
    long getNextTimestamp(long afterTimestamp);

    int getLocalMinimum(float fromTimestampPosition, float toTimestampPosition);
    int getLocalMaximum(float fromTimestampPosition, float toTimestampPosition);

    String getYStampText(int value);
    String getXStampText(long timestamp);
}
