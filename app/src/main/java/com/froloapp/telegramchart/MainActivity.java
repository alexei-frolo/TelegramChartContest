package com.froloapp.telegramchart;

import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;

import java.io.InputStream;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity
        implements LineChartSlider.OnScrollListener, LineChartView.OnStampClickListener {

    private Spinner spinnerChartSelector;
    private LineChartView chartView;
    private LineChartSlider chartSlider;
    private LinearLayout layoutCheckboxes;

    private LineChartAdapter adapter;

    // hold task to cancel if needed
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
        // first check if night mode enabled
//        PrefManager manager = PrefManager.getInstance(this);
//        boolean nightModeEnabled = manager.isNightModeEnabled();
//        applyDayNightMode(nightModeEnabled);

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
        this.adapter = adapter;

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
    public void onStampClick(final LineChartView view, float x, float y, float rawX, float rawY, long timestamp) {
        final View v = getLayoutInflater().inflate(R.layout.dialog_stamp_info, null, false);
        int index = adapter.getTimestampIndex(timestamp);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        int month = c.get(Calendar.MONTH);
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        String title = Utils.getDayOfWeekString(dayOfWeek) + ", " + Utils.getMonthString(month) + ' ' + dayOfMonth;
        ((TextView) v.findViewById(R.id.textStamp)).setText(title);

        LinearLayout layoutValues = v.findViewById(R.id.layoutValues);
        for (int i = 0; i < adapter.getLineCount(); i++) {
            Line line = adapter.getLineAt(i);
            if (adapter.isLineEnabled(line)) {
                final int hp = (int) Utils.dpToPx(2f, this);
                final int vp = (int) Utils.dpToPx(2f, this);
                final String text = line.getName() + "\n" + line.getValueAt(index);
                TextView textView = new TextView(this);
                textView.setTextColor(line.getColor());
                textView.setText(text);
                textView.setPadding(hp, vp, hp, vp);
                layoutValues.addView(textView);
            }
        }

        final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        final PopupWindow popUp = new PopupWindow(v, w, h);
        Rect location = Utils.getViewLocation(chartView);
        if (location == null) {
            // early return. Shouldn't happen
            return;
        }
        popUp.setTouchable(true);
        popUp.setFocusable(true);
        popUp.setOutsideTouchable(true);
        popUp.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override public void onDismiss() {
                view.clearClickedStamp();
            }
        });

        final int locX = location.left + (int) x + 15; // + 15 to make a margin between x axis bar and dialog
        final int locY = location.top;
        popUp.showAtLocation(chartView, Gravity.TOP | Gravity.LEFT, locX, locY);
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
        log("Switching DayNight mode");
        PrefManager manager = PrefManager.getInstance(this);
        boolean nightModeEnabled = manager.isNightModeEnabled();
        manager.setNightModeEnabled(!nightModeEnabled);
        applyDayNightMode(!nightModeEnabled);
    }

    private void applyDayNightMode(boolean nightModeEnabled) {
        log("Applying DayNight mode: [night=" + nightModeEnabled + "]");
        @AppCompatDelegate.NightMode int mode = nightModeEnabled ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate delegate = getDelegate();
        delegate.setLocalNightMode(mode);
        delegate.applyDayNight();
    }
}
