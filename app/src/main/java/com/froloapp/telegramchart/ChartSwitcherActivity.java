package com.froloapp.telegramchart;


import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;


public class ChartSwitcherActivity extends AbsChartActivity
        implements LineChartView.OnStampClickListener, LineChartSlider.OnScrollListener {

    private Spinner spinnerCharts;
    private LineChartView chartView;
    private LineChartSlider chartSlider;
    private LinearLayout layoutCheckboxes;

    private LineChartAdapter currAdapter;

    private PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_switcher);
        spinnerCharts = findViewById(R.id.spinnerCharts);
        chartView = findViewById(R.id.chartView);
        chartSlider = findViewById(R.id.chartSlider);
        layoutCheckboxes = findViewById(R.id.layoutCheckboxes);
        load();
    }

    @Override
    void populateCharts(LineChartAdapter[] adapters) {
        initSpinner(adapters);
    }

    private void initSpinner(LineChartAdapter[] adapters) {
        ArrayAdapter<LineChartAdapter> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        spinnerAdapter.addAll(adapters);
        spinnerCharts.setAdapter(spinnerAdapter);
        if (adapters.length != 0) {
            LineChartAdapter firstChart = adapters[0];
            initChart(firstChart, false);
            spinnerCharts.setSelection(0);
        }
        // set callback after set selection
        spinnerCharts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LineChartAdapter adapter = (LineChartAdapter) parent.getAdapter().getItem(position);
                initChart(adapter, true);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void initChart(LineChartAdapter adapter, boolean animate) {
        currAdapter = adapter;

        chartView.setOnStampClickListener(null);
        chartSlider.setOnScrollListener(null);

        final float startXPosition = 0.0f;
        final float stopXPosition = 0.3f;

        chartView.setOnStampClickListener(this);
        chartView.setAdapter(adapter, animate);
        chartView.setXPositions(startXPosition, stopXPosition, animate);

        chartSlider.setAdapter(adapter, animate);
        chartSlider.setXPositions(startXPosition, stopXPosition, animate);
        chartSlider.setOnScrollListener(this);

        initCheckboxes(adapter);
    }

    private void initCheckboxes(final LineChartAdapter adapter) {
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
            checkBox.setChecked(adapter.isLineEnabled(line));
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
            int m = (int) Utils.dpToPx(5f, this);
            lp.leftMargin = m;
            lp.topMargin = m;
            lp.rightMargin = m;
            lp.bottomMargin = m;
            checkBox.setMinimumWidth((int) Utils.dpToPx(70f, this));
            layoutCheckboxes.addView(checkBox, lp);
        }
    }

    @Override
    public void onScroll(LineChartSlider slider, float startStampRel, float endStampRel) {
        chartView.setXPositions(startStampRel, endStampRel, true);
    }

    @Override
    public void onTouchDown(final LineChartView view, long timestamp, float timestampX) {
        showPopup(timestamp, timestampX);
    }

    @Override
    public void onTouchUp(LineChartView view) {
        PopupWindow currWindow = this.popupWindow;
        if (currWindow != null) {
            currWindow.dismiss();
            this.popupWindow = null;
        }
    }

    private void showPopup(long timestamp, float timestampX) {
        final Rect location = Utils.getViewLocation(chartView);
        if (location == null) {
            // early return. Shouldn't happen
            return;
        }

        final int locX = location.left
                + (int) timestampX
                + 15; // + 15 to make a margin between x axis bar and dialog
        final int locY = location.top;

        final PopupWindow currPopup = this.popupWindow;
        if (currPopup == null) {
            PopupWindow newWindow = PopupHelper.createPopupWindow(this, currAdapter, timestamp);
            if (newWindow != null) {
                newWindow.showAtLocation(chartView, Gravity.TOP | Gravity.LEFT, locX, locY);
                this.popupWindow = newWindow;
            }
        } else {
            final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
            final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
            View v = currPopup.getContentView();
            PopupHelper.populateWindowView(v, currAdapter, timestamp);
            currPopup.update(locX, locY, w, h);
        }
    }
}
