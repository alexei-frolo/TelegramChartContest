package com.froloapp.telegramchart.widget.linechartview;


/**
 * Represents UI for a line chart view
 */
public interface LineChartUI {
    void setAdapter(LineChartAdapter adapter, boolean animate);

    // sets start and stop x positions in percentage (value in range 0..1)
    void setXPositions(float start, float stop, boolean animate);

    void show(Line chart, boolean animate);
    void hide(Line chart, boolean animate);
}
