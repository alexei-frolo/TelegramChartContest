package com.froloapp.telegramchart;


import android.graphics.Color;
import android.os.AsyncTask;

import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.factory.LineChartFactory;

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
        void onResult(LineChartAdapter[] adapters);
        void onCancelled();
    }

    private final Callback callback;
    private LineChartAdapter[] result;

    JsonParserTask(Callback callback) {
        this.callback = callback;
    }

    LineChartAdapter[] getResult() {
        return result;
    }

    @Override
    protected void onPreExecute() {
        callback.onStart();
    }

    @Override
    protected void onPostExecute(Object o) {
        if (o instanceof LineChartAdapter[]) {
            this.result = (LineChartAdapter[]) o;
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
            List<LineChartAdapter> allAdapters = new ArrayList<>();
            for (InputStream is : streams) {
                String json = parseJson(is);
                List<LineChartAdapter> adapters = parseAdapters(json);
                allAdapters.addAll(adapters);
            }
            return allAdapters.toArray(new LineChartAdapter[] { });
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
    private List<LineChartAdapter> parseAdapters(String json) throws Throwable {
        JSONArray array = new JSONArray(json);
        int count = array.length();
        List<LineChartAdapter> adapters = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            JSONObject obj = array.getJSONObject(i);
            LineChartAdapter a = parseAdapter(obj);
            adapters.add(a);
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

        LineChartFactory.Builder builder = LineChartFactory.builder(timestamps);
        for (int i = 0; i < chartCount; i++) {
            JSONArray columns = columnsJson.getJSONArray(i);
            String chartCode = columns.get(0).toString();
            if (chartCode.equals("x")) { // continue
            } else {
                int[] values = new int[columns.length()];
                for (int j = 1; j < columns.length(); j++) {
                    int value = columns.getInt(j);
                    values[j - 1] = value;
                }
                String type = typesJson.getString(chartCode); // what to do with this??
                String name = namesJson.getString(chartCode);
                String color = colorsJson.getString(chartCode);

                builder.addLine(values, Color.parseColor(color), name);
            }
        }
        return builder.build();
    }
}
