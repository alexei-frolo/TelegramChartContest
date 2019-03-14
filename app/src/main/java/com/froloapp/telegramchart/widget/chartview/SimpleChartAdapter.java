package com.froloapp.telegramchart.widget.chartview;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleChartAdapter implements ChartAdapter {
    private List<Long> timestamps;
    private long mixXAxis;
    private long maxXAxis;

    private List<ChartHolder> chartHolders;
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

    public SimpleChartAdapter(List<Long> timestamps, List<ChartData> charts) {
        this.timestamps = timestamps;
        Collections.sort(timestamps); // default sort
        this.mixXAxis = timestamps.get(0);
        this.maxXAxis = timestamps.get(timestamps.size() - 1);
        this.chartHolders = new ArrayList<>(charts.size());
        for (ChartData data : charts) {
            chartHolders.add(new ChartHolder(data, true));
        }

        // save local minimums nad maximums
        calcMinimumsAndMaximums();
    }

    private void calcMinimumsAndMaximums() {
        localMinimums.clear();
        localMaximums.clear();
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            int minValue = findMinValue(timestamp);
            int maxValue = findMaxValue(timestamp);
            localMinimums.put(timestamp, minValue);
            localMaximums.put(timestamp, maxValue);
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

    @Override
    public boolean hasNextTimestamp(long afterXAxis) {
        int index = timestamps.indexOf(afterXAxis);
        return index >= 0 && index < timestamps.size() - 1;
    }

    @Override
    public long getNextTimestamp(long afterXAxis) {
        int index = timestamps.indexOf(afterXAxis);
        return timestamps.get(index + 1);
    }

    // finds min value for the given timestamp
    private int findMinValue(long timestamp) {
        int min = Integer.MAX_VALUE;
        for (ChartHolder holder : chartHolders) {
            if (!holder.visible) continue;

            int value = holder.data.getValue(timestamp);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    // finds max value for the given timestamp
    private int findMaxValue(long timestamp) {
        int max = Integer.MIN_VALUE;
        for (ChartHolder holder : chartHolders) {
            if (!holder.visible) continue;

            int value = holder.data.getValue(timestamp);
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
            int min = findMaxValue(timestamp);
            localMinimums.put(timestamp, min);
            return min;
        }
    }

    private int getMaxValue(long timestamp) {
        Integer v = localMaximums.get(timestamp);
        if (v != null) {
            return v;
        } else {
            int max = findMaxValue(timestamp);
            localMaximums.put(timestamp, max);
            return max;
        }
    }

    @Override
    public int getMinYValue(float fromXAxisRel, float toXAxisRel) {
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
    public int getMaxXValue(float fromXAxisRel, float toXAxisRel) {
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
    public boolean hasNextTimestamp(float timestampRel) {
        return timestampRel < 1f;
    }

    @Override
    public long getNextTimestamp(float timestampRel) {
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * timestampRel));
        for (long axis : timestamps) {
            if (axis > approximatelyDesiredAxis)
                return axis;
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + timestampRel);
    }

    @Override
    public float getNextTimestampPosition(float timestampRel) {
        long minAxis = timestamps.get(0);
        long maxAxis = timestamps.get(timestamps.size() - 1);
        //long desiredAxis = (minAxis + (long) ((maxAxis - minAxis) * timestampRel)) + 1;
        float approximatelyDesiredAxis = (minAxis + ((maxAxis - minAxis) * timestampRel));
        for (long axis : timestamps) {
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
        // think of much more optimized way
        calcMinimumsAndMaximums();
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
