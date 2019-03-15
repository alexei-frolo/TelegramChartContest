package com.froloapp.telegramchart.widget.linechartview.factory;


import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.Line;

import java.util.Arrays;
import java.util.List;

public final class LineChartAdapters {
    private LineChartAdapters() {
    }

    public static LineChartAdapter create(List<Long> timestamps, List<Line> lines) {
        return new SimpleLineChartAdapter(timestamps, lines);
    }

    public static LineChartAdapter create(List<Long> timestamps, Line... lines) {
        return new SimpleLineChartAdapter(timestamps, Arrays.asList(lines));
    }
}
