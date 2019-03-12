package com.froloapp.telegramchart.widget.chartview;


import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SimpleChartAdapter implements ChartAdapter {
    private List<Long> axes;
    private long mixXAxis;
    private long maxXAxis;

    private List<ChartData> charts;

    public SimpleChartAdapter(List<Long> axes, List<ChartData> charts) {
        this.axes = axes;
        Collections.sort(axes); // default sort
        this.mixXAxis = axes.get(0);
        this.maxXAxis = axes.get(axes.size() - 1);
        this.charts = charts;
    }

    public static class SimpleData implements ChartData {
        private Map<Long, Integer> data;
        private int color;

        public SimpleData(Map<Long, Integer> data, int color) {
            this.data = data;
            this.color = color;
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
    }

    @Override
    public long getMinTimestamp() {
        return mixXAxis;
    }

    @Override
    public long getMaxTimestamp() {
        return maxXAxis;
    }

    @Override
    public long[] getTimestamps(float fromXPosition, float toXPosition) {
        return new long[0];
    }

    @Override
    public boolean hasNextTimestamp(long afterXAxis) {
        int index = axes.indexOf(afterXAxis);
        return index >= 0 && index < axes.size() - 1;
    }

    @Override
    public long getNextTimestamp(long afterXAxis) {
        int index = axes.indexOf(afterXAxis);
        return axes.get(index + 1);
    }

    @Override
    public int getMinYValue(float fromXAxisRel, float toXAxisRel) {
        long startAxis = axes.get(0);
        long endAxis = axes.get(axes.size() - 1);
        long fromXAxis = (long) (startAxis + (endAxis - startAxis) * fromXAxisRel);
        long toXAxis = (long) (startAxis + (endAxis - startAxis) * toXAxisRel);
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis < fromXAxis) {
                if (i < axes.size() - 1) {
                    // check if the next axis is in bounds
                    long nextAxis = axes.get(i + 1);
                    if (nextAxis < fromXAxis) {
                        for (ChartData data : charts) {
                            int value = data.getValue(axis);
                            if (value < min) {
                                min = value;
                            }
                        }
                    }
                }
                continue;
            }
            if (axis > toXAxis) {
                for (ChartData data : charts) {
                    int value = data.getValue(axis);
                    if (value < min) {
                        min = value;
                    }
                }
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
    public int getMaxXValue(float fromXAxisRel, float toXAxisRel) {
        long startAxis = axes.get(0);
        long endAxis = axes.get(axes.size() - 1);
        long fromXAxis = (long) (startAxis + (endAxis - startAxis) * fromXAxisRel);
        long toXAxis = (long) (startAxis + (endAxis - startAxis) * toXAxisRel);
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis < fromXAxis) {
                if (i < axes.size() - 1) {
                    // check if the next axis is in bounds
                    long nextAxis = axes.get(i + 1);
                    if (nextAxis < fromXAxis) {
                        for (ChartData data : charts) {
                            int value = data.getValue(axis);
                            if (value > max) {
                                max = value;
                            }
                        }
                    }
                }
                continue;
            }
            if (axis > toXAxis) {
                for (ChartData data : charts) {
                    int value = data.getValue(axis);
                    if (value > max) {
                        max = value;
                    }
                }
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
    public boolean hasPreviousTimestamp(float beforeTimestampPosition) {
        return beforeTimestampPosition > 0f;
    }

    @Override
    public long getPreviousTimestamp(float previousTimestampPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * previousTimestampPosition)) + 1;
        for (int i = axes.size() - 1; i >=0; i--) {
            long axis = axes.get(i);
            if (axis <= desiredAxis)
                return axis;
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + previousTimestampPosition);
    }

    @Override
    public float getPreviousTimestampPosition(float previousTimestampPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * previousTimestampPosition)) + 1;
        for (int i = axes.size() - 1; i >=0; i--) {
            long axis = axes.get(i);
            if (axis <= desiredAxis)
                return ((float) (axis - minAxis)) / (maxAxis - minAxis);
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + previousTimestampPosition);
    }

    @Override
    public boolean hasNextTimestamp(float timestampRel) {
        return timestampRel < 1f;
    }

    @Override
    public long getNextTimestamp(float timestampRel) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        for (long axis : axes) {
            if (axis >= desiredAxis)
                return axis;
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + timestampRel);
    }

    @Override
    public float getNextTimestampPosition(float timestampRel) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        for (long axis : axes) {
            if (axis >= desiredAxis)
                return ((float) (axis - minAxis)) / (maxAxis - minAxis);
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + timestampRel);
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
