package com.froloapp.telegramchart.widget.chartview.factory;


import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


class SimpleChartAdapter implements ChartAdapter {
    private static AtomicInteger chartId = new AtomicInteger(0);

    private static int nextChartId() {
        return chartId.getAndAdd(1);
    }

    private final int id;

    private List<Long> timestamps;
    private long firstTimestamp;
    private long lastTimestamp;

    private List<ChartHolder> chartHolders = new ArrayList<>();
    private final Map<Long, Integer> localMinimums = new HashMap<>();
    private final Map<Long, Integer> localMaximums = new HashMap<>();

    private static class ChartHolder {
        final ChartData data;
        boolean visible;
        ChartHolder(ChartData data, boolean visible) {
            this.data = data;
            this.visible = visible;
        }
    }

    SimpleChartAdapter(List<Long> timestamps, List<ChartData> charts) {
        id = nextChartId();

        chartHolders.clear();
        if (!timestamps.isEmpty()) {
            this.timestamps = timestamps;
            this.firstTimestamp = timestamps.get(0);
            this.lastTimestamp = timestamps.get(timestamps.size() - 1);
            Collections.sort(timestamps); // default sort
            for (ChartData data : charts) {
                chartHolders.add(new ChartHolder(data, true));
            }
        }

        // save local minimums nad maximums
        calcMinimumsAndMaximums();
    }

    private void calcMinimumsAndMaximums() {
        localMinimums.clear();
        localMaximums.clear();
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            int minValue = findMinValueAt(i);
            int maxValue = findMaxValueAt(i);
            localMinimums.put(timestamp, minValue);
            localMaximums.put(timestamp, maxValue);
        }
    }

    @Override
    public long getFirstTimestamp() {
        return firstTimestamp;
    }

    @Override
    public long getLastTimestamp() {
        return lastTimestamp;
    }

    @Override
    public int getTimestampCount() {
        return timestamps.size();
    }

    @Override
    public int getTimestampIndex(long timestamp) {
        return timestamps.indexOf(timestamp);
    }

    @Override
    public float getClosestTimestampPosition(float toXPosition) {
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * toXPosition));
        for (int i = 0; i < timestamps.size(); i++) {
            long axis = timestamps.get(i);
            if (axis > approximatelyDesiredAxis) {
                float next = ((float) (axis - minAxis)) / (maxAxis - minAxis);
                if (i > 0) {
                    long previousAxis = timestamps.get(i - 1);
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
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * toXPosition));
        for (int i = 0; i < timestamps.size(); i++) {
            long axis = timestamps.get(i);
            if (axis > approximatelyDesiredAxis) {
                float next = ((float) (axis - minAxis)) / (maxAxis - minAxis);
                if (i > 0) {
                    long previousAxis = timestamps.get(i - 1);
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

    // finds min value for the given timestamp
    private int findMinValueAt(int index) {
        int min = Integer.MAX_VALUE;
        for (ChartHolder holder : chartHolders) {
            if (!holder.visible) continue;

            int value = holder.data.getValueAt(index);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    // finds max value for the given timestamp
    private int findMaxValueAt(int index) {
        int max = Integer.MIN_VALUE;
        for (ChartHolder holder : chartHolders) {
            if (!holder.visible) continue;

            int value = holder.data.getValueAt(index);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private int getMinValue(long timestamp) {
        Integer v = localMinimums.get(timestamp);
        if (v != null) {
            return v;
        } else {
            int index = getTimestampIndex(timestamp);
            int min = findMaxValueAt(index);
            localMinimums.put(timestamp, min);
            return min;
        }
    }

    private int getMaxValue(long timestamp) {
        Integer v = localMaximums.get(timestamp);
        if (v != null) {
            return v;
        } else {
            int index = getTimestampIndex(timestamp);
            int max = findMaxValueAt(index);
            localMaximums.put(timestamp, max);
            return max;
        }
    }

    @Override
    public int getLocalMinimum(float fromXAxisRel, float toXAxisRel) {
        long startTimestamp = timestamps.get(0);
        long stopTimestamp = timestamps.get(timestamps.size() - 1);
        long fromTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * fromXAxisRel) - 1;
        long toTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * toXAxisRel) + 1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            if (timestamp < fromTimestamp) {
                if (i < timestamps.size() - 1) {
                    // check if the next axis is in the bounds
                    long nextTimestamp = timestamps.get(i + 1);
                    if (nextTimestamp >= fromTimestamp) {
                        int localMin = getMinValue(timestamp);
                        if (localMin < min) {
                            min = localMin;
                        }
                    }
                }
                continue;
            }
            if (timestamp > toTimestamp) {
                int localMin = getMinValue(timestamp);
                if (localMin < min) {
                    min = localMin;
                }
                break;
            }
            int localMin = getMinValue(timestamp);
            if (localMin < min) {
                min = localMin;
            }
        }
        return min;
    }

    @Override
    public int getLocalMaximum(float fromXAxisRel, float toXAxisRel) {
        long startTimestamp = timestamps.get(0);
        long stopTimestamp = timestamps.get(timestamps.size() - 1);
        long fromTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * fromXAxisRel) - 1;
        long toTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * toXAxisRel) + 1;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            if (timestamp < fromTimestamp) {
                if (i < timestamps.size() - 1) {
                    // check if the next axis is in the bounds
                    long nextTimestamp = timestamps.get(i + 1);
                    if (nextTimestamp >= fromTimestamp) {
                        int localMax = getMaxValue(timestamp);
                        if (localMax > max) {
                            max = localMax;
                        }
                    }
                }
                continue;
            }
            if (timestamp > toTimestamp) {
                int localMax = getMaxValue(timestamp);
                if (localMax > max) {
                    max = localMax;
                }
                break;
            }
            int localMax = getMaxValue(timestamp);
            if (localMax > max) {
                max = localMax;
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
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * beforeTimestampPosition)) - 1;
        for (int i = timestamps.size() - 1; i >=0; i--) {
            long axis = timestamps.get(i);
            if (axis <= desiredAxis)
                return axis;
        }
        return timestamps.get(0);
    }

    @Override
    public float getPreviousTimestampPosition(float beforeTimestampPosition) {
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * beforeTimestampPosition)) - 1;
        for (int i = timestamps.size() - 1; i >=0; i--) {
            long axis = timestamps.get(i);
            if (axis <= desiredAxis)
                return ((float) (axis - minAxis)) / (maxAxis - minAxis);
        }
        return 0f;
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
        // think of much more optimized way
        calcMinimumsAndMaximums();
    }

    @Override
    public String getYStampText(int value) {
        return String.valueOf(value);
    }

    @Override
    public String getXStampText(long timestamp) {
        return String.valueOf(timestamp);
    }

    @Override
    public String toString() {
        return "Simple chart #" + String.valueOf(id);
    }
}
