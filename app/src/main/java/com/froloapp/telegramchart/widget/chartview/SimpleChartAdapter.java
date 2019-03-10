package com.froloapp.telegramchart.widget.chartview;


import java.util.List;
import java.util.Map;

public class SimpleChartAdapter implements ChartAdapter {
    private List<Long> axes;
    private long mixXAxis;
    private long maxXAxis;

    private List<ChartData> charts;

    public SimpleChartAdapter(List<Long> axes, List<ChartData> charts) {
        this.axes = axes;
        this.mixXAxis = axes.get(0);
        this.maxXAxis = axes.get(axes.size() - 1);
        this.charts = charts;
    }

    public static class SimpleData implements ChartData {
        private Map<Long, Integer> data;

        public SimpleData(Map<Long, Integer> data) {
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

    @Override
    public long getMinXAxis() {
        return mixXAxis;
    }

    @Override
    public long getMaxXAxis() {
        return maxXAxis;
    }

    @Override
    public int getMinValue(long fromXAxis, long toXAxis) {
        int fromXAxisIndex = axes.indexOf(fromXAxis);
        int min = Integer.MAX_VALUE;
        for (int i = fromXAxisIndex; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis > toXAxis) {
                break;
            }
            for (ChartData data : charts) {
                int value = data.getValue(axis);
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    @Override
    public int getMaxValue(long fromXAxis, long toXAxis) {
        int fromXAxisIndex = axes.indexOf(fromXAxis);
        int max = Integer.MIN_VALUE;
        for (int i = fromXAxisIndex; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis > toXAxis) {
                break;
            }
            for (ChartData data : charts) {
                int value = data.getValue(axis);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    @Override
    public boolean hasNextAxis(long afterXAxis) {
        int index = axes.indexOf(afterXAxis);
        return index >= 0 && index < axes.size() - 1;
    }

    @Override
    public long getNextAxis(long afterXAxis) {
        int index = axes.indexOf(afterXAxis);
        return axes.get(index + 1);
    }

    @Override
    public int getChartCount() {
        return charts.size();
    }

    @Override
    public ChartData getChart(int index) {
        return charts.get(index);
    }
}
