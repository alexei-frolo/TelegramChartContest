package com.froloapp.telegramchart;


import android.content.res.ColorStateList;
import android.os.Build;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;

class ChartListAdapter extends BaseAdapter {
    private final LineChartAdapter[] adapters;

    ChartListAdapter(LineChartAdapter[] adapters) {
        this.adapters = adapters;
    }

    @Override
    public int getCount() {
        return adapters.length;
    }

    @Override
    public LineChartAdapter getItem(int position) {
        return adapters[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.item_chart, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        LineChartAdapter adapter = getItem(position);
        holder.bind(adapter);
        return convertView;
    }

    private static class ViewHolder implements LineChartView.OnStampClickListener, LineChartSlider.OnScrollListener {
        final View rootView;
        final LineChartView chartView;
        final LineChartSlider chartSlider;
        final LinearLayout layoutCheckboxes;

        ViewHolder(View view) {
            rootView = view;
            chartView = view.findViewById(R.id.chartView);
            chartSlider = view.findViewById(R.id.chartSlider);
            layoutCheckboxes = view.findViewById(R.id.layoutCheckboxes);
        }

        void bind(LineChartAdapter adapter) {
            initChart(adapter);
            initCheckboxes(adapter);
        }

        private void initChart(LineChartAdapter adapter) {
            chartView.setOnStampClickListener(null);
            chartSlider.setOnScrollListener(null);

            final float startXPosition = 0.0f;
            final float stopXPosition = 0.3f;

            chartView.setOnStampClickListener(this);
            chartView.setAdapter(adapter);
            chartView.setXPositions(startXPosition, stopXPosition);

            chartSlider.setAdapter(adapter);
            chartSlider.setXPositions(startXPosition, stopXPosition);
            chartSlider.setOnScrollListener(this);
        }

        private void initCheckboxes(final LineChartAdapter adapter) {
            layoutCheckboxes.removeAllViews();
            // adding checkboxes dynamic
            for (int i = 0; i < adapter.getLineCount(); i++) {
                final Line line = adapter.getLineAt(i);
                final int color = line.getColor();
                AppCompatCheckBox checkBox = new AppCompatCheckBox(rootView.getContext());
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
                int m = (int) Utils.dpToPx(5f, rootView.getContext());
                lp.leftMargin = m;
                lp.topMargin = m;
                lp.rightMargin = m;
                lp.bottomMargin = m;
                checkBox.setMinimumWidth((int) Utils.dpToPx(70f, rootView.getContext()));
                layoutCheckboxes.addView(checkBox, lp);
            }
        }

        @Override
        public void onScroll(LineChartSlider slider, float startStampRel, float endStampRel) {
            chartView.setXPositions(startStampRel, endStampRel);
        }

        @Override
        public void onStampClick(final LineChartView view, long timestamp, float timestampX) {
        }
    }
}
