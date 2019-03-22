package com.froloapp.telegramchart;

import android.os.Bundle;
import android.widget.ListView;

import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;


public class ChartListActivity extends AbsChartActivity {

    private ListView listCharts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_list);
        listCharts = findViewById(R.id.listCharts);
        load();
    }

    @Override
    void populateCharts(LineChartAdapter[] adapters) {
        ChartListAdapter chartListAdapter = new ChartListAdapter(adapters);
        listCharts.setAdapter(chartListAdapter);
    }
}
