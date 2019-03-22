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
        private final String name;
        private final List<Long> timestamps;
        private final List<Line> lines = new ArrayList<>();
        private Builder(String name, List<Long> timestamps) {
            if (timestamps == null) {
                throw new IllegalArgumentException("Timestamps must not be null");
            }
            this.name = name;
            this.timestamps = timestamps;
        }

        public Builder addLine(int[] values, int color, String name) {
            Line line = new IndexedLine(values, color, name);
            lines.add(line);
            return this;
        }

        public LineChartAdapter build() {
            return new SimpleLineChartAdapter(name, timestamps, lines);
        }
    }

    public static Builder builder(String name, List<Long> timestamps) {
        return new Builder(name, timestamps);
    }
}
