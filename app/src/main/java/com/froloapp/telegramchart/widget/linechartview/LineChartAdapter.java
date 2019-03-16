package com.froloapp.telegramchart.widget.linechartview;


// Should be as effective as possible
public interface LineChartAdapter {
    int getLineCount();
    Line getLineAt(int index);
    boolean isLineEnabled(Line line);
    void setLineEnabled(Line line, boolean enabled);

    int getTimestampCount();
    long getTimestampAt(int index);
    int getLeftClosestTimestampIndex(float toXPosition);
    float getTimestampRelPositionAt(int index);
    int getTimestampIndex(long timestamp);

    float getClosestTimestampPosition(float toXPosition);
    long getClosestTimestamp(float toXPosition);

    int getLocalMinimum(float fromTimestampPosition, float toTimestampPosition);
    int getLocalMaximum(float fromTimestampPosition, float toTimestampPosition);

    String getYStampText(int value);
    String getXStampTextAt(int index);
}
