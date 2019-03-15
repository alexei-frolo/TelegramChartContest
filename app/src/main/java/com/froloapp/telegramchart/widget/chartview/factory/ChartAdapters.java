package com.froloapp.telegramchart.widget.chartview.factory;


import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;

import java.util.List;

public final class ChartAdapters {
    private ChartAdapters() {
    }

    public static ChartAdapter create(List<Long> timestamps, List<ChartData> charts) {
        return new SimpleChartAdapter(timestamps, charts);
    }
}
