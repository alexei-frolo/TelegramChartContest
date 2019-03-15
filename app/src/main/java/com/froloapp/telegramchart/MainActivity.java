package com.froloapp.telegramchart;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;

import java.io.InputStream;

public class MainActivity extends Activity implements LineChartSlider.OnScrollListener, LineChartView.OnStampClickListener {

    private Spinner spinnerChartSelector;
    private LineChartView chartView;
    private LineChartSlider chartSlider;
    private LinearLayout layoutCheckboxes;

    // hold task to cancel if needed
    private JsonParserTask jsonParserTask;

    private void log(Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e("ChartViewMainActivity", "", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinnerChartSelector = findViewById(R.id.spinnerChartSelector);
        chartView = findViewById(R.id.chartView);
        chartSlider = findViewById(R.id.chartSlider);
        layoutCheckboxes = findViewById(R.id.layoutCheckboxes);

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
            @Override public void onResult(LineChartAdapter[] adapters) {
                initSpinner(adapters);
                if (adapters.length >= 1) {
                    spinnerChartSelector.setSelection(0);
                }
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

    private void initSpinner(final LineChartAdapter[] adapters) {
        ArrayAdapter<LineChartAdapter> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(adapters);
        spinnerChartSelector.setAdapter(adapter);
        spinnerChartSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LineChartAdapter a = adapters[position];
                initChart(a);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void initCheckboxes(final LineChartAdapter adapter) {
        layoutCheckboxes.removeAllViews();
        // adding checkboxes dynamic
        for (int i = 0; i < adapter.getLineCount(); i++) {
            final Line chart = adapter.getLineAt(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(chart.getName());
            checkBox.setChecked(adapter.isLineEnabled(chart));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        chartView.show(chart);
                        chartSlider.show(chart);
                    } else {
                        chartView.hide(chart);
                        chartSlider.hide(chart);
                    }
                }
            });
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int m = (int) Utils.dpToPx(5f, this);
            lp.leftMargin = m;
            lp.topMargin = m;
            lp.rightMargin = m;
            lp.bottomMargin = m;
            layoutCheckboxes.addView(checkBox, lp);
        }
    }

    private void initChart(LineChartAdapter adapter) {
        initCheckboxes(adapter);

        final float startXPosition = 0.0f;
        final float stopXPosition = 0.3f;

        chartView.setOnStampClickListener(MainActivity.this);
        chartView.setAdapter(adapter);
        chartView.setXPositions(startXPosition, stopXPosition);

        chartSlider.setAdapter(adapter);
        chartSlider.setXPositions(startXPosition, stopXPosition);
        chartSlider.setOnScrollListener(MainActivity.this);
    }

    @Override
    public void onScroll(LineChartSlider slider, float startStampRel, float endStampRel) {
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
