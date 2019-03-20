package com.froloapp.telegramchart.widget.linechartview;


// Should be as effective as possible
public interface LineChartAdapter {
    int getLineCount();
    Line getLineAt(int index);
    boolean isLineEnabled(Line line);
    void setLineEnabled(Line line, boolean enabled);
    int getEnabledLineCount();

    int getTimestampCount();
    long getTimestampAt(int index);
    int getLeftClosestTimestampIndex(float toXPosition);
    float getTimestampRelPosition(long timestamp);
    float getTimestampRelPositionAt(int index);
    int getTimestampIndex(long timestamp);

    float getClosestTimestampPosition(float toXPosition);
    long getClosestTimestamp(float toXPosition);

    MinMaxValue getLocalMinMax(float fromTimestampPosition, float toTimestampPosition);

    String getYStampText(int value);
    String getXStampTextAt(int index);

    class MinMaxValue {
        public int min;
        public int max;
    }
}
