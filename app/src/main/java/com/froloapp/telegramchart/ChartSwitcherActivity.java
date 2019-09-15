package com.froloapp.telegramchart;


import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;

import com.froloapp.telegramchart.widget.Chart;
import com.froloapp.telegramchart.widget.ChartView;
import com.froloapp.telegramchart.widget.Line;
import com.froloapp.telegramchart.widget.Misc;
import com.froloapp.telegramchart.widget.ChartSlider;


public class ChartSwitcherActivity extends AbsChartActivity
        implements ChartSlider.OnScrollListener,
        ChartView.OnStampClickListener {

    private static final String LOG_TAG = "ChartSwitcherActivity";

    private Spinner spinnerCharts;
    private ChartView chartView;
    private ChartSlider chartSlider;
    private LinearLayout layoutCheckboxes;

    // I need to know when spinner callback triggered due a user input or programmatically
    private boolean spinnerTouched = false;

    private PopupWindow popupWindow;

    private void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_switcher);
        spinnerCharts = findViewById(R.id.spinnerCharts);
        chartView = findViewById(R.id.chartView);
        chartSlider = findViewById(R.id.chartSlider);
        layoutCheckboxes = findViewById(R.id.layoutCheckboxes);

        spinnerCharts.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    spinnerTouched = true; // user touched the spinner
                }
                return false;
            }
        });
        // set callback after set selection
        spinnerCharts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Chart chart = (Chart) parent.getAdapter().getItem(position);
                initChart(chart, spinnerTouched); // animate only if selected by user
                spinnerTouched = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        final float startXPosition = 0.0f;
        final float stopXPosition = 0.3f;

        chartView.setOnStampClickListener(this);
        chartView.setXPositions(startXPosition, stopXPosition, false);
        chartSlider.setXPositions(startXPosition, stopXPosition, false);
        chartSlider.setOnScrollListener(this);

        load();
    }

    @Override
    void populateCharts(Chart[] charts) {
        initSpinner(charts);
    }

    private void initSpinner(Chart[] charts) {
        ArrayAdapter<Chart> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        spinnerAdapter.addAll(charts);
        spinnerCharts.setAdapter(spinnerAdapter);
        if (charts.length != 0) {
            Chart firstChart = charts[0];
            initChart(firstChart, false);
            spinnerCharts.setSelection(0);
        }
    }

    private void initChart(Chart chart, boolean animate) {
        chartView.setChart(chart, animate);
        chartSlider.setChart(chart, animate);
        initCheckboxes(chart);
    }

    private void initCheckboxes(final Chart adapter) {
        layoutCheckboxes.removeAllViews();
        // adding checkboxes dynamic
        for (int i = 0; i < adapter.getLineCount(); i++) {
            final Line line = adapter.getLineAt(i);
            final int color = line.getColor();
            AppCompatCheckBox checkBox = new AppCompatCheckBox(this);
            if (Build.VERSION.SDK_INT >= 21) {
                checkBox.setButtonTintList(ColorStateList.valueOf(color));
            } else {
                ((TintableCompoundButton) checkBox).setSupportButtonTintList(ColorStateList.valueOf(color));
            }
            checkBox.setText(line.getName());
            checkBox.setChecked(chartView.isLineVisible(line));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        chartView.show(line, true);
                        chartSlider.show(line, true);
                    } else {
                        chartView.hide(line, true);
                        chartSlider.hide(line, true);
                    }
                }
            });
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int m = (int) Misc.dpToPx(5f, this);
            lp.leftMargin = m;
            lp.topMargin = m;
            lp.rightMargin = m;
            lp.bottomMargin = m;
            checkBox.setMinimumWidth((int) Misc.dpToPx(70f, this));
            layoutCheckboxes.addView(checkBox, lp);
        }
    }

    @Override
    public void onScroll(ChartSlider slider, float startStampRel, float endStampRel) {
        chartView.setXPositions(startStampRel, endStampRel, true);
    }

    @Override
    public void onTouchDown(final ChartView view, int stampIndex, float stampXCoordinate) {
        showPopup(view, stampIndex, stampXCoordinate);
    }

    @Override
    public void onTouchUp(ChartView view) {
        PopupWindow currWindow = this.popupWindow;
        if (currWindow != null) {
            currWindow.dismiss();
            this.popupWindow = null;
        }
    }

    private void showPopup(ChartView chartView, int stampIndex, float stampXCoordinate) {
        final Rect location = Utils.getViewLocation(chartView);
        if (location == null) {
            // early return. Shouldn't happen
            return;
        }

        final int locX = location.left
                + (int) stampXCoordinate
                + 15; // + 15 to make a margin between x axis bar and dialog
        final int locY = location.top;

        final PopupWindow currPopup = this.popupWindow;
        if (currPopup == null) {
            PopupWindow newWindow = PopupHelper.createPopupWindow(chartView, stampIndex);
            if (newWindow != null) {
                newWindow.showAtLocation(chartView, Gravity.TOP | Gravity.START, locX, locY);
                this.popupWindow = newWindow;
            }
        } else {
            final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
            final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
            View v = currPopup.getContentView();
            PopupHelper.populateWindowView(v, chartView, stampIndex);
            currPopup.update(locX, locY, w, h);
        }
    }
}
