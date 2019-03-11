package com.froloapp.telegramchart;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;


import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;
import com.froloapp.telegramchart.widget.chartview.ChartSlider;
import com.froloapp.telegramchart.widget.chartview.ChartView;
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

public class MainActivity extends AppCompatActivity implements ChartSlider.OnScrollListener {

    private ChartView chartView;
    private ChartAdapter adapter;
    private ChartSlider chartSlider;
    private CheckBox checkboxFirst;
    private CheckBox checkboxSecond;

    private ChartData firstChart;
    private ChartData secondChart;

    private void log(String msg) {
        Log.d("MainActivity", msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chartView = findViewById(R.id.chartView);
        chartSlider = findViewById(R.id.chartSlider);
        checkboxFirst = findViewById(R.id.checkboxFirst);
        checkboxSecond = findViewById(R.id.checkboxSecond);

        checkboxFirst.setChecked(true);
        checkboxFirst.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) chartView.show(firstChart);
                else chartView.hide(firstChart);
            }
        });

        checkboxSecond.setChecked(true);
        checkboxSecond.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) chartView.show(secondChart);
                else chartView.hide(secondChart);
            }
        });

        loadChartData();
    }

    private void loadChartData() {
        AssetManager assets = getAssets();
        try {
            InputStream is = assets.open("chart.json");
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
                    //SimpleTimedAdapter.Timestamp t = new SimpleTimedAdapter.Timestamp(timestamp, )
                }
                int color;
                if (k == 0) {
                    color = Color.RED;
                } else {
                    color = Color.BLUE;
                }
                ChartData chart = new SimpleChartAdapter.SimpleData(map, color);
                charts.add(chart);
                if (k == 0) {
                    firstChart = chart;
                } else {
                    secondChart = chart;
                }
            }

            List<Long> axes = new ArrayList<>(axesSet);
            adapter = new SimpleChartAdapter(axes, charts);
            chartView.setAdapter(adapter);
            chartView.setXPositions(0f, 1f);

            chartSlider.setPositions(0f, 1f);
            chartSlider.setOnScrollListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onScroll(ChartSlider slider, float startStampRel, float endStampRel) {
        log("Scrolled: startStampRel=" + startStampRel + ", endStampRel=" + endStampRel);
        chartView.setXPositions(startStampRel, endStampRel);
    }
}
