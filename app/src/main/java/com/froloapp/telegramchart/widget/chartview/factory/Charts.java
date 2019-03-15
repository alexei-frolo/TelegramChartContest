package com.froloapp.telegramchart.widget.chartview.factory;


import com.froloapp.telegramchart.widget.chartview.ChartData;

import java.util.Map;

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

    private static class MappedData extends AbsChartData {
        final Map<Long, Integer> data;
        MappedData(Map<Long, Integer> data, int color, String name) {
            super(color, name);
            this.data = data;
        }
        @Override public int getValue(long x) {
            Integer value = data.get(x);
            if (value == null) {
                throw new IllegalArgumentException("No such x axis found");
            }
            return value;
        }
    }

    public interface Function {
        int get(long stamp);
    }

    private static class FuncData extends AbsChartData {
        final Function func;
        FuncData(Function func, int color, String name) {
            super(color, name);
            this.func = func;
        }
        @Override public int getValue(long x) {
            return func.get(x);
        }
    }

    public static ChartData create(Map<Long, Integer> data, int color, String name) {
        return new MappedData(data, color, name);
    }

    public static ChartData create(Function func, int color, String name) {
        return new FuncData(func, color, name);
    }
}
