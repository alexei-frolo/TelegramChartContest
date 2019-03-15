package com.froloapp.telegramchart;


import android.graphics.Color;
import android.os.AsyncTask;

import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.factory.LineChartAdapters;
import com.froloapp.telegramchart.widget.linechartview.factory.Lines;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a json string from an input stream and parses it into chart data
 */
public class JsonParserTask extends AsyncTask<InputStream, Void, Object> {

    interface Callback {
        void onStart();
        void onError(Throwable error);
        void onResult(LineChartAdapter[] adapters);
        void onCancelled();
    }

    private final Callback callback;

    JsonParserTask(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        callback.onStart();
    }

    @Override
    protected void onPostExecute(Object o) {
        if (o instanceof LineChartAdapter[]) {
            callback.onResult((LineChartAdapter[]) o);
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
            final InputStream is = streams[0];
            String json = parseJson(is);
            LineChartAdapter[] adapters = parseAdapters(json);
            return adapters;
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
    private LineChartAdapter[] parseAdapters(String json) throws Throwable {
        JSONArray array = new JSONArray(json);
        int count = array.length();
        LineChartAdapter[] adapters = new LineChartAdapter[count];
        for (int i = 0; i < count; i++) {
            JSONObject obj = array.getJSONObject(i);
            adapters[i] = parseAdapter(obj);
        }
        return adapters;
    }

    // parses json object into chart adapter
    private LineChartAdapter parseAdapter(JSONObject obj) throws Throwable {
        JSONArray columnsJson = obj.getJSONArray("columns");
        JSONObject typesJson = obj.getJSONObject("types");
        JSONObject namesJson = obj.getJSONObject("names");
        JSONObject colorsJson = obj.getJSONObject("colors");

        int chartCount = columnsJson.length();

        // collect timestamps first
        List<Long> timestamps = new ArrayList<>();
        for (int i = 0; i < chartCount; i++) {
            JSONArray columns = columnsJson.getJSONArray(i);
            String chartCode = columns.get(0).toString();
            if (chartCode.equals("x")) { // these are timestamps
                for (int j = 1; j < columns.length(); j++) {
                    long stamp = columns.getLong(j);
                    timestamps.add(stamp);
                }
            }
        }

        List<Line> charts = new ArrayList<>();
        for (int i = 0; i < chartCount; i++) {
            JSONArray columns = columnsJson.getJSONArray(i);
            String chartCode = columns.get(0).toString();
            if (chartCode.equals("x")) { // continue
            } else {
                List<Integer> values = new ArrayList<>();
                Map<Long, Integer> map = new LinkedHashMap<>();
                for (int j = 1; j < columns.length(); j++) {
                    int value = columns.getInt(j);
                    long stamp = timestamps.get(j - 1);
                    map.put(stamp, value);
                    //
                    values.add(value);
                }
                String type = typesJson.getString(chartCode); // what to do with this??
                String name = namesJson.getString(chartCode);
                String color = colorsJson.getString(chartCode);
                //Line data = Lines.create(map, Color.parseColor(color), name);
                //charts.add(data);
                Line data = Lines.create(values, Color.parseColor(color), name);
                charts.add(data);
            }
        }
        return LineChartAdapters.create(timestamps, charts);
    }
}
