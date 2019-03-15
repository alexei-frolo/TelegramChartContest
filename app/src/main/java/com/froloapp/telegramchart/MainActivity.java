package com.froloapp.telegramchart;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.chartview.ChartAdapter;
import com.froloapp.telegramchart.widget.chartview.ChartData;
import com.froloapp.telegramchart.widget.chartview.ChartSlider;
import com.froloapp.telegramchart.widget.chartview.ChartView;

import java.io.InputStream;

public class MainActivity extends Activity implements ChartSlider.OnScrollListener, ChartView.OnStampClickListener {

    private ChartView chartView;
    private ChartAdapter adapter;
    private ChartSlider chartSlider;
    private CheckBox checkboxFirst;
    private CheckBox checkboxSecond;

    private ChartData firstChart;
    private ChartData secondChart;

    private JsonParserTask jsonParserTask;

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartViewMainActivity", msg);
        }
    }

    private void log(Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e("ChartViewMainActivity", "", e);
        }
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
                if (isChecked) {
                    chartView.show(firstChart);
                    chartSlider.show(firstChart);
                } else {
                    chartView.hide(firstChart);
                    chartSlider.hide(firstChart);
                }
            }
        });

        checkboxSecond.setChecked(true);
        checkboxSecond.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    chartView.show(secondChart);
                    chartSlider.show(secondChart);
                } else {
                    chartView.hide(secondChart);
                    chartSlider.hide(secondChart);
                }
            }
        });

        loadCharts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);
    }

    private void loadCharts() {
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);

        JsonParserTask newTask = new JsonParserTask(new JsonParserTask.Callback() {
            @Override public void onStart() {
                // show progress here
            }
            @Override public void onError(Throwable error) {
                log(error);
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
            @Override public void onResult(ChartAdapter[] adapters) {
                ChartAdapter adapter = adapters[0];
                MainActivity.this.adapter = adapter;

                if (adapter.getChartCount() >= 2) {
                    ChartData first = adapter.getChart(0);
                    checkboxFirst.setHighlightColor(first.getColor());
                    firstChart = first;

                    ChartData second = adapter.getChart(1);
                    checkboxSecond.setHighlightColor(second.getColor());
                    secondChart = second;
                }

                float startXPosition = 0.0f;
                float stopXPosition = 0.3f;

                chartView.setOnStampClickListener(MainActivity.this);
                chartView.setAdapter(adapter);
                chartView.setXPositions(startXPosition, stopXPosition);

                chartSlider.setAdapter(adapter);
                chartSlider.setBorderPositions(startXPosition, stopXPosition);
                chartSlider.setOnScrollListener(MainActivity.this);
            }
            @Override
            public void onCancelled() {
            }
        });
        jsonParserTask = newTask;
        AssetManager assets = getAssets();
        try {
            InputStream is = assets.open("chart_data.json");
            newTask.execute(is);
        } catch (Throwable e) {
            log(e);
        }
    }

    @Override
    public void onScroll(ChartSlider slider, float startStampRel, float endStampRel) {
        log("Scrolled: startStampRel=" + startStampRel + ", endStampRel=" + endStampRel);
        chartView.setXPositions(startStampRel, endStampRel);
    }

    @Override
    public void onStampClick(float x, float y, float rawX, float rawY, long xAxis) {
//            final View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_stamp_info, null, false);
//            ((TextView) v.findViewById(R.id.textStamp)).setText(String.valueOf(closestXAxis));
//            final PopupWindow popUp = new PopupWindow(v, 100, 100);
//            Rect location = Utils.getViewLocation(this);
//            popUp.setTouchable(true);
//            popUp.setFocusable(true);
//            popUp.setOutsideTouchable(true);
//            popUp.showAtLocation(this, Gravity.END, -(int) x, +(int) y);
    }
}
