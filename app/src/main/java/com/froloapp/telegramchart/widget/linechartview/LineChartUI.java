package com.froloapp.telegramchart.widget.linechartview;


/**
 * Represents UI for a line chart view
 */
public interface LineChartUI {
    void setAdapter(LineChartAdapter adapter);

    float getStartXPosition();

    float getStopXPosition();

    // sets start x position in percentage (value in range 0..1)
    void setStartXPosition(float p);

    // sets stop x position in percentage (value in range 0..1)
    void setStopXPosition(float p);

    // sets start and stop x positions in percentage (value in range 0..1)
    void setXPositions(float start, float stop);

    void show(Line chart);
    void hide(Line chart);
}
