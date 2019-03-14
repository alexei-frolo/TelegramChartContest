package com.froloapp.telegramchart.widget.chartview;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SimpleChartAdapter implements ChartAdapter {
    private List<Long> axes;
    private long mixXAxis;
    private long maxXAxis;

    private List<ChartHolder> chartHolders;

    private static class ChartHolder {
        final ChartData data;
        boolean visible;
        ChartHolder(ChartData data, boolean visible) {
            this.data = data;
            this.visible = visible;
        }
    }

    public SimpleChartAdapter(List<Long> axes, List<ChartData> charts) {
        this.axes = axes;
        Collections.sort(axes); // default sort
        this.mixXAxis = axes.get(0);
        this.maxXAxis = axes.get(axes.size() - 1);
        this.chartHolders = new ArrayList<>(charts.size());
        for (ChartData data : charts) {
            chartHolders.add(new ChartHolder(data, true));
        }
    }

    public static class SimpleData implements ChartData {
        private Map<Long, Integer> data;
        private int color;
        private String name;

        public SimpleData(Map<Long, Integer> data, int color, String name) {
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
    public float getClosestTimestampPosition(float toXPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * toXPosition));
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis > approximatelyDesiredAxis) {
                float next = ((float) (axis - minAxis)) / (maxAxis - minAxis);
                if (i > 0) {
                    long previousAxis = axes.get(i - 1);
                    float previous = ((float) (previousAxis - minAxis)) / (maxAxis - minAxis);
                    if (Math.abs(previous - toXPosition) < Math.abs(next - toXPosition)) {
                        return previous;
                    } else {
                        return next;
                    }
                } else {
                    return next;
                }
            }
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + toXPosition);
    }

    @Override
    public long getClosestTimestamp(float toXPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * toXPosition));
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis > approximatelyDesiredAxis) {
                float next = ((float) (axis - minAxis)) / (maxAxis - minAxis);
                if (i > 0) {
                    long previousAxis = axes.get(i - 1);
                    float previous = ((float) (previousAxis - minAxis)) / (maxAxis - minAxis);
                    if (Math.abs(previous - toXPosition) < Math.abs(next - toXPosition)) {
                        return previousAxis;
                    } else {
                        return axis;
                    }
                } else {
                    return axis;
                }
            }
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + toXPosition);
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
        long fromXAxis = (long) (startAxis + (endAxis - startAxis) * fromXAxisRel) - 1;
        long toXAxis = (long) (startAxis + (endAxis - startAxis) * toXAxisRel) + 1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis < fromXAxis) {
                if (i < axes.size() - 1) {
                    // check if the next axis is in bounds
                    long nextAxis = axes.get(i + 1);
                    if (nextAxis >= fromXAxis) {
                        for (ChartHolder holder : chartHolders) {
                            if (!holder.visible) continue;

                            int value = holder.data.getValue(axis);
                            if (value < min) {
                                min = value;
                            }
                        }
                    }
                }
                continue;
            }
            if (axis > toXAxis) {
                for (ChartHolder holder : chartHolders) {
                    if (!holder.visible) continue;

                    int value = holder.data.getValue(axis);
                    if (value < min) {
                        min = value;
                    }
                }
                break;
            }
            for (ChartHolder holder : chartHolders) {
                if (!holder.visible) continue;

                int value = holder.data.getValue(axis);
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
        long fromXAxis = (long) (startAxis + (endAxis - startAxis) * fromXAxisRel) - 1;
        long toXAxis = (long) (startAxis + (endAxis - startAxis) * toXAxisRel) + 1;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < axes.size(); i++) {
            long axis = axes.get(i);
            if (axis < fromXAxis) {
                if (i < axes.size() - 1) {
                    // check if the next axis is in bounds
                    long nextAxis = axes.get(i + 1);
                    if (nextAxis >= fromXAxis) {
                        for (ChartHolder holder : chartHolders) {
                            if (!holder.visible) continue;

                            int value = holder.data.getValue(axis);
                            if (value > max) {
                                max = value;
                            }
                        }
                    }
                }
                continue;
            }
            if (axis > toXAxis) {
                for (ChartHolder holder : chartHolders) {
                    if (!holder.visible) continue;

                    int value = holder.data.getValue(axis);
                    if (value > max) {
                        max = value;
                    }
                }
                break;
            }
            for (ChartHolder holder : chartHolders) {
                if (!holder.visible) continue;

                int value = holder.data.getValue(axis);
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
    public long getPreviousTimestamp(float beforeTimestampPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * beforeTimestampPosition)) - 1;
        for (int i = axes.size() - 1; i >=0; i--) {
            long axis = axes.get(i);
            if (axis <= desiredAxis)
                return axis;
        }
        return axes.get(0);
    }

    @Override
    public float getPreviousTimestampPosition(float beforeTimestampPosition) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * beforeTimestampPosition)) - 1;
        for (int i = axes.size() - 1; i >=0; i--) {
            long axis = axes.get(i);
            if (axis <= desiredAxis)
                return ((float) (axis - minAxis)) / (maxAxis - minAxis);
        }
        return 0f;
    }

    @Override
    public boolean hasNextTimestamp(float timestampRel) {
        return timestampRel < 1f;
    }

    @Override
    public long getNextTimestamp(float timestampRel) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * timestampRel));
        for (long axis : axes) {
            if (axis > approximatelyDesiredAxis)
                return axis;
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + timestampRel);
    }

    @Override
    public float getNextTimestampPosition(float timestampRel) {
        long minAxis = axes.get(0);
        long maxAxis = axes.get(axes.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * timestampRel));
        for (long axis : axes) {
            if (axis > approximatelyDesiredAxis)
                return ((float) (axis - minAxis)) / (maxAxis - minAxis);
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + timestampRel);
    }

    @Override
    public int getChartCount() {
        return chartHolders.size();
    }

    @Override
    public ChartData getChart(int index) {
        return chartHolders.get(index).data;
    }

    @Override
    public boolean isVisible(ChartData chart) {
        for (int i = 0; i < chartHolders.size(); i++) {
            ChartHolder holder = chartHolders.get(i);
            if (holder.data.equals(chart)) {
                return holder.visible;
            }
        }
        return false;
    }

    @Override
    public void setVisible(ChartData chart, boolean visible) {
        for (int i = 0; i < chartHolders.size(); i++) {
            ChartHolder holder = chartHolders.get(i);
            if (holder.data.equals(chart)) {
                holder.visible = visible;
            }
        }
    }

    @Override
    public String getYBarText(int value) {
        return String.valueOf(value);
    }

    @Override
    public String getXStampText(long timestamp) {
        return String.valueOf(timestamp);
    }
}
