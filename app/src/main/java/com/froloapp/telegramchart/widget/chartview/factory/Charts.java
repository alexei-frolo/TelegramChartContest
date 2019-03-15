package com.froloapp.telegramchart.widget.chartview.factory;


import com.froloapp.telegramchart.widget.chartview.ChartData;

import java.util.List;

public final class Charts {
    private Charts() {
    }

    private static abstract class AbsChartData implements ChartData {
        final int color;
        final String name;
        AbsChartData(int color, String name) {
            this.color = color;
            this.name = name;
        }
        @Override public int getColor() {
            return color;
        }
        @Override public String getName() {
            return name;
        }
    }

    private static class IndexedData extends AbsChartData {
        final List<Integer> data;
        IndexedData(List<Integer> data, int color, String name) {
            super(color, name);
            this.data = data;
        }
        @Override public int getValueAt(int index) {
            return data.get(index);
        }
    }

    public static ChartData create(List<Integer> data, int color, String name) {
        return new IndexedData(data, color, name);
    }
}
