package com.froloapp.telegramchart.widget.chartview.factory;


import com.froloapp.telegramchart.widget.chartview.ChartData;

import java.util.Map;

public final class Charts {
    private Charts() {
    }

    private static class SimpleData implements ChartData {
        private Map<Long, Integer> data;
        private int color;
        private String name;

        SimpleData(Map<Long, Integer> data, int color, String name) {
            this.data = data;
            this.color = color;
            this.name = name;
        }

        @Override public int getColor() {
            return color;
        }
        @Override public int getValue(long x) {
            Integer value = data.get(x);
            if (value == null) {
                throw new IllegalArgumentException("No such x axis found");
            }
            return value;
        }
        @Override public String getName() {
            return name;
        }
    }

    public static ChartData create(Map<Long, Integer> data, int color, String name) {
        return new SimpleData(data, color, name);
    }
}
