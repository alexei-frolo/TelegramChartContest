package com.froloapp.telegramchart.widget.linechartview.factory;


import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


class SimpleLineChartAdapter implements LineChartAdapter {
    // static
    private static final int DEFAULT_MIN_VALUE = -10;
    private static final int DEFAULT_MAX_VALUE = 10;

    private static AtomicInteger chartId = new AtomicInteger(0);

    private static int nextChartId() {
        return chartId.getAndAdd(1);
    }

    private final int id;

    private final List<Long> timestamps = new ArrayList<>();
    private final List<String> timestampTexts = new ArrayList<>();

    private List<LineHolder> lineHolders = new ArrayList<>();
    private final List<Integer> localMinimums = new ArrayList<>(); // local minimums at indexes
    private final List<Integer> localMaximums = new ArrayList<>(); // local maximums at indexes

    private final MinMaxValue minMaxValueHolder = new MinMaxValue();

    private static class LineHolder {
        final Line data;
        boolean visible;
        LineHolder(Line data, boolean visible) {
            this.data = data;
            this.visible = visible;
        }
    }

    SimpleLineChartAdapter(List<Long> timestamps, List<Line> lines) {
        id = nextChartId();

        lineHolders.clear();
        this.timestamps.addAll(timestamps);
        if (!timestamps.isEmpty()) {
            Collections.sort(timestamps); // default sort
            for (Line data : lines) {
                lineHolders.add(new LineHolder(data, true));
            }
        }

        // save local minimums nad maximums
        calcMinimumsAndMaximums();
        obtainTimestampTexts();
    }

    private void obtainTimestampTexts() {
        timestampTexts.clear();
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timestamp);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            String text = Utils.getMonthString(month) + ' ' + day;
            timestampTexts.add(text);
        }
    }

    private void calcMinimumsAndMaximums() {
        localMinimums.clear();
        localMaximums.clear();
        for (int i = 0; i < timestamps.size(); i++) {
            int minValue = findMinValueAt(i);
            int maxValue = findMaxValueAt(i);
            localMinimums.add(minValue);
            localMaximums.add(maxValue);
        }
    }

    @Override
    public int getTimestampCount() {
        return timestamps.size();
    }

    @Override
    public long getTimestampAt(int index) {
        return timestamps.get(index);
    }

    @Override
    public int getLeftClosestTimestampIndex(float toXPosition) {
        if (timestamps.isEmpty()) return -1; // early return

        long minTimestamp = timestamps.get(0);
        long maxTimestamp = timestamps.get(timestamps.size() - 1);
        long approximatelyTimestamp = minTimestamp + (long) ((maxTimestamp - minTimestamp) * toXPosition);
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            if (timestamp >= approximatelyTimestamp) {
                return Math.max(0, i - 2); // it's a hack. Must be Math.max(0, i - 1)
            }
        }
        return 0;
    }

    @Override
    public float getTimestampRelPosition(long timestamp) {
        int index = timestamps.indexOf(timestamp);
        return getTimestampRelPositionAt(index);
    }

    @Override
    public float getTimestampRelPositionAt(int index) {
        long timestamp = timestamps.get(index);
        long minTimestamp = timestamps.get(0);
        long maxTimestamp = timestamps.get(timestamps.size() - 1);
        return ((float) (timestamp - minTimestamp)) / (maxTimestamp - minTimestamp);
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
        float approximateTimestamp = (minAxis + ((maxAxis - minAxis) * toXPosition));
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            if (timestamp > approximateTimestamp) {
                if (i > 0) {
                    float timestampXPosition = ((float) (timestamp - minAxis)) / (maxAxis - minAxis);
                    long previousTimestamp = timestamps.get(i - 1);
                    float previousTimestampXPosition = ((float) (previousTimestamp - minAxis)) / (maxAxis - minAxis);
                    if (Math.abs(previousTimestampXPosition - toXPosition) < Math.abs(timestampXPosition - toXPosition)) {
                        return previousTimestamp;
                    } else {
                        return timestamp;
                    }
                } else {
                    return timestamp;
                }
            }
        }
        throw new IllegalArgumentException("Invalid timestamp rel: " + toXPosition);
    }

    // finds min value for the given timestamp
    private int findMinValueAt(int index) {
        boolean atLeastOneLineEnabled = false;
        int min = Integer.MAX_VALUE;
        for (LineHolder holder : lineHolders) {
            if (!holder.visible) continue;

            atLeastOneLineEnabled = true;
            int value = holder.data.getValueAt(index);
            if (value < min) {
                min = value;
            }
        }
        if (atLeastOneLineEnabled) return min;
        else return 0;
    }

    // finds max value for the given timestamp
    private int findMaxValueAt(int index) {
        boolean atLeastOneLineEnabled = false;
        int max = Integer.MIN_VALUE;
        for (LineHolder holder : lineHolders) {
            if (!holder.visible) continue;

            atLeastOneLineEnabled = true;
            int value = holder.data.getValueAt(index);
            if (value > max) {
                max = value;
            }
        }
        if (atLeastOneLineEnabled) return max;
        else return 10;
    }

    private int getMinValueAt(int index) {
        return localMinimums.get(index);
    }

    private int getMaxValueAt(int index) {
        return localMaximums.get(index);
    }

    @Override
    public MinMaxValue getLocalMinMax(float fromTimestampPosition, float toTimestampPosition) {
        long startTimestamp = timestamps.get(0);
        long stopTimestamp = timestamps.get(timestamps.size() - 1);
        long fromTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * fromTimestampPosition) - 1;
        long toTimestamp = (long) (startTimestamp + (stopTimestamp - startTimestamp) * toTimestampPosition) + 1;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            if (timestamp < fromTimestamp) {
                if (i < timestamps.size() - 1) {
                    // check if the next axis is in the bounds
                    long nextTimestamp = timestamps.get(i + 1);
                    if (nextTimestamp >= fromTimestamp) {
                        int localMin = getMinValueAt(i);
                        if (localMin < min) {
                            min = localMin;
                        }
                        int localMax = getMaxValueAt(i);
                        if (localMax > max) {
                            max = localMax;
                        }
                    }
                }
                continue;
            }
            if (timestamp > toTimestamp) {
                int localMin = getMinValueAt(i);
                if (localMin < min) {
                    min = localMin;
                }
                int localMax = getMaxValueAt(i);
                if (localMax > max) {
                    max = localMax;
                }
                break;
            }
            int localMin = getMinValueAt(i);
            if (localMin < min) {
                min = localMin;
            }
            int localMax = getMaxValueAt(i);
            if (localMax > max) {
                max = localMax;
            }
        }
        minMaxValueHolder.min = min;
        minMaxValueHolder.max = max;
        return minMaxValueHolder;
    }

    @Override
    public int getLineCount() {
        return lineHolders.size();
    }

    @Override
    public Line getLineAt(int index) {
        return lineHolders.get(index).data;
    }

    @Override
    public boolean isLineEnabled(Line chart) {
        for (int i = 0; i < lineHolders.size(); i++) {
            LineHolder holder = lineHolders.get(i);
            if (holder.data.equals(chart)) {
                return holder.visible;
            }
        }
        return false;
    }

    @Override
    public void setLineEnabled(Line chart, boolean visible) {
        for (int i = 0; i < lineHolders.size(); i++) {
            LineHolder holder = lineHolders.get(i);
            if (holder.data.equals(chart)) {
                holder.visible = visible;
                for (int j = 0; j < timestamps.size(); j++) {
                    int value = holder.data.getValueAt(j);
                    int currMinValue = localMinimums.get(j);
                    int currMaxValue = localMaximums.get(j);
                    if (visible) {
                        if (value < currMinValue) {
                            localMinimums.set(j, value);
                        }
                        if (value > currMaxValue) {
                            localMaximums.set(j, value);
                        }
                    } else {
                        if (value <= currMinValue) {
                            int newMinValue = findMinValueAt(j);
                            localMinimums.set(j, newMinValue);
                        }
                        if (value >= currMaxValue) {
                            int newMaxValue = findMaxValueAt(j);
                            localMaximums.set(j, newMaxValue);
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public int getEnabledLineCount() {
        int count = 0;
        for (LineHolder holder : lineHolders) {
            if (holder.visible) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getYStampText(int value) {
        return Utils.format(value);
    }

    @Override
    public String getXStampTextAt(int index) {
        return timestampTexts.get(index);
    }

    @Override
    public String toString() {
        return "Line chart #" + String.valueOf(id);
    }
}
