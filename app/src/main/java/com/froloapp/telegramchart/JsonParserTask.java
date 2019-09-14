package com.froloapp.telegramchart;


import android.graphics.Color;
import android.os.AsyncTask;

import com.froloapp.telegramchart.widget.Chart;
import com.froloapp.telegramchart.widget.Line;
import com.froloapp.telegramchart.widget.Point;
import com.froloapp.telegramchart.widget.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a json string from an input stream and parses it into chart data
 */
public class JsonParserTask extends AsyncTask<InputStream, Void, Object> {

    interface Callback {
        void onStart();
        void onError(Throwable error);
        void onResult(Chart[] charts);
        void onCancelled();
    }

    private final Callback callback;
    private Chart[] result;

    JsonParserTask(Callback callback) {
        this.callback = callback;
    }

    Chart[] getResult() {
        return result;
    }

    @Override
    protected void onPreExecute() {
        callback.onStart();
    }

    @Override
    protected void onPostExecute(Object o) {
        if (o instanceof Chart[]) {
            this.result = (Chart[]) o;
            callback.onResult((Chart[]) o);
        } else if (o instanceof Throwable) {
            callback.onError((Throwable) o);
        }
    }

    @Override
    protected void onCancelled() {
        callback.onCancelled();
    }

    @Override
    protected Object doInBackground(InputStream... streams) {
        try {
            List<Chart> allAdapters = new ArrayList<>();
            for (InputStream is : streams) {
                String json = parseJson(is);
                List<Chart> adapters = parseCharts(json);
                allAdapters.addAll(adapters);
            }
            return allAdapters.toArray(new Chart[] { });
        } catch (Throwable t) {
            return t;
        }
    }

    // parses input stream into json string
    private String parseJson(InputStream is) throws Throwable {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(is, "UTF-8");
        for (;; ) {
            int rsz = in.read(buffer, 0, buffer.length);
            if (rsz < 0)
                break;
            out.append(buffer, 0, rsz);
        }
        return out.toString();
    }

    // parses json string into chart adapters
    private List<Chart> parseCharts(String json) throws Throwable {
        JSONArray array = new JSONArray(json);
        int count = array.length();
        List<Chart> adapters = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            JSONObject obj = array.getJSONObject(i);
            Chart a = parseChart(i, obj);
            adapters.add(a);
        }
        return adapters;
    }

    // parses json object into chart adapter
    private Chart parseChart(int index, JSONObject obj) throws Throwable {
        JSONArray columnsJson = obj.getJSONArray("columns");
        JSONObject typesJson = obj.getJSONObject("types");
        JSONObject namesJson = obj.getJSONObject("names");
        JSONObject colorsJson = obj.getJSONObject("colors");

        int chartCount = columnsJson.length();

        // collect timestamps first
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < chartCount; i++) {
            JSONArray columns = columnsJson.getJSONArray(i);
            String chartCode = columns.get(0).toString();
            if (chartCode.equals("x")) { // these are timestamps
                for (int j = 1; j < columns.length(); j++) {
                    long stamp = columns.getLong(j);
                    String text = "point";
                    Point point = new Point(stamp, text);
                    points.add(point);
                }
            }
        }

        String chartName = "OldLine chart #" + index;

        List<Line> lines = new ArrayList<>();

        for (int i = 0; i < chartCount; i++) {
            JSONArray columns = columnsJson.getJSONArray(i);
            String chartCode = columns.get(0).toString();
            if (chartCode.equals("x")) { // continue
            } else {
                float[] values = new float[columns.length()];
                for (int j = 1; j < columns.length(); j++) {
                    int value = columns.getInt(j);
                    values[j - 1] = value;
                }
                String type = typesJson.getString(chartCode); // what to do with this??
                String name = namesJson.getString(chartCode);
                String color = colorsJson.getString(chartCode);

                Line line = new Line(values, name, Color.parseColor(color));

                lines.add(line);
            }
        }
        return Chart.create(points, lines);
    }
}
