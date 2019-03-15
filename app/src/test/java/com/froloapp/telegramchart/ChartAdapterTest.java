package com.froloapp.telegramchart;

import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;
import com.froloapp.telegramchart.widget.chartview.factory.ChartAdapters;
import com.froloapp.telegramchart.widget.chartview.factory.Charts;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class ChartAdapterTest {
    // src
    private final long[] timestampsSrc = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private final int[] chart1src = { 73, 75, 111, 137, 115, 107, 105, 123, 81, 59 };
    private final int[] chart2src = { 35, 23, 170, 150, 139, 34, 57, 71, 51, 63 };

    private List<Long> timestamps;
    private ChartData chart1;
    private ChartData chart2;
    private ChartAdapter adapter;

    @Before
    public void setup() {
        // init timestamps
        timestamps = new ArrayList<>();
        for (long t : timestampsSrc) timestamps.add(t);

        // init chart 1
        Map<Long, Integer> map1 = new HashMap<>();
        for (int i = 0; i < timestamps.size(); i++) map1.put(timestamps.get(i), chart1src[i]);
        chart1 = Charts.create(map1, 0xFFFFFF, "Chart 1");

        // init chart 1
        Map<Long, Integer> map2 = new HashMap<>();
        for (int i = 0; i < timestamps.size(); i++) map2.put(timestamps.get(i), chart2src[i]);
        chart2 = Charts.create(map2, 0xFFFFFF, "Chart 2");

        adapter = ChartAdapters.create(timestamps, chart1, chart2);
    }

    @Test
    public void test_ChartCount() {
        assertEquals(adapter.getChartCount(), 2);
    }
}