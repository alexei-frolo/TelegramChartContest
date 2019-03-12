package com.froloapp.telegramchart;


import android.graphics.Color;
import android.os.AsyncTask;

import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;
import com.froloapp.telegramchart.widget.chartview.SimpleChartAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a json string from an input stream and parses it into chart data
 */
public class JsonParserTask extends AsyncTask<InputStream, Void, Object> {

    interface Callback {
        void onStart();
        void onError(Throwable error);
        void onResult(ChartAdapter adapter);
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
    protected Object doInBackground(InputStream... streams) {
        try {
            final InputStream is = streams[0];
            final int bufferSize = 1024;
            final char[] buffer = new char[bufferSize];
            final StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(is, "UTF-8");
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
            String json = out.toString();
            JSONArray array = new JSONArray(json);

            Set<Long> axesSet = new LinkedHashSet<>();
            List<ChartData> charts = new ArrayList<>();

            for (int k = 0; k < array.length(); k++) {
                JSONArray dataArray = array.getJSONArray(k);
                Map<Long, Integer> map = new LinkedHashMap<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject obj = dataArray.getJSONObject(i);
                    long timestamp = obj.getLong("timestamp");
                    int value = obj.getInt("value");
                    axesSet.add(timestamp);
                    map.put(timestamp, value);
                }
                int color;
                if (k == 0) {
                    color = Color.RED;
                } else {
                    color = Color.BLUE;
                }
                ChartData chart = new SimpleChartAdapter.SimpleData(map, color);
                charts.add(chart);
            }

            List<Long> axes = new ArrayList<>(axesSet);
            return new SimpleChartAdapter(axes, charts);
        } catch (Throwable t) {
            return t;
        }
    }

    @Override
    protected void onPostExecute(Object o) {
        if (o instanceof ChartAdapter) {
            callback.onResult((ChartAdapter) o);
        } else if (o instanceof Throwable) {
            callback.onError((Throwable) o);
        }
    }

    @Override
    protected void onCancelled() {
        callback.onCancelled();
    }
}
