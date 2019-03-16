package com.froloapp.telegramchart;

import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;

import java.util.List;



public class ChartAdapterTest {
    // src
    private final long[] timestampsSrc = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private final int[] chart1src = { 73, 75, 111, 137, 115, 107, 105, 123, 81, 59 };
    private final int[] chart2src = { 35, 23, 170, 150, 139, 34, 57, 71, 51, 63 };

    private List<Long> timestamps;
    private Line chart1;
    private Line chart2;
    private LineChartAdapter adapter;

}