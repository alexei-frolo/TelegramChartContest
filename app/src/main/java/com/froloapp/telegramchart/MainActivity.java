package com.froloapp.telegramchart;

import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity
        implements LineChartSlider.OnScrollListener, LineChartView.OnStampClickListener {

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
            final Line line = adapter.getLineAt(i);
            final int color = line.getColor();
            AppCompatCheckBox checkBox = new AppCompatCheckBox(this);
            if (Build.VERSION.SDK_INT < 21) {
                CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(color));
            } else {
                checkBox.setButtonTintList(ColorStateList.valueOf(color));
            }
            checkBox.setText(line.getName());
            checkBox.setChecked(adapter.isLineEnabled(line));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        chartView.show(line);
                        chartSlider.show(line);
                    } else {
                        chartView.hide(line);
                        chartSlider.hide(line);
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
            checkBox.setMinimumWidth((int) Utils.dpToPx(70f, this));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSwitchDatNightMode: {
                switchDayNightMode();
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void switchDayNightMode() {
        PrefManager manager = PrefManager.getInstance(this);
        boolean nightModeEnabled = manager.isNightModeEnabled();
        manager.setNightModeEnabled(!nightModeEnabled);
        @AppCompatDelegate.NightMode int nextMode = nightModeEnabled ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate delegate = getDelegate();
        delegate.setLocalNightMode(nextMode);
        delegate.applyDayNight();
        recreate();
    }
}
