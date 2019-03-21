package com.froloapp.telegramchart.widget.linechartview.factory;


import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;

import java.util.ArrayList;
import java.util.List;

public final class LineChartFactory {
    private LineChartFactory() { }

    // Default implementation
    private static class IndexedLine implements Line {
        final int color;
        final String name;
        final int[] values;
        IndexedLine(int[] values, int color, String name) {
            this.color = color;
            this.name = name;
            this.values = values;
        }
        @Override public int getColor() {
            return color;
        }
        @Override public String getName() {
            return name;
        }
        @Override public int getValueAt(int index) {
            return values[index];
        }
    }

    public static class Builder {
        private List<Long> timestamps;
        private List<Line> lines = new ArrayList<>();
        private Builder() {
        }

        public Builder setTimestamps(List<Long> timestamps) {
            this.timestamps = timestamps;
            return this;
        }

        public Builder addLine(int[] values, int color, String name) {
            Line line = new IndexedLine(values, color, name);
            lines.add(line);
            return this;
        }

        public LineChartAdapter build() {
            if (timestamps == null) {
                throw new IllegalArgumentException("Timestamps must not be null");
            }
            return new SimpleLineChartAdapter(timestamps, lines);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
