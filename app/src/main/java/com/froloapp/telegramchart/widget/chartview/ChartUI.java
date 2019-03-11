package com.froloapp.telegramchart.widget.chartview;


/**
 * Represents UI for a chart view
 */
public interface ChartUI {
    void setAdapter(ChartAdapter adapter);

    // sets start x position in percentage (value in range 0..1)
    void setStartXPosition(float p);

    // sets stop x position in percentage (value in range 0..1)
    void setStopXPosition(float p);

    // sets start and stop x positions in percentage (value in range 0..1)
    void setXPositions(float start, float stop);

    void show(ChartData chart);
    void hide(ChartData chart);
}
