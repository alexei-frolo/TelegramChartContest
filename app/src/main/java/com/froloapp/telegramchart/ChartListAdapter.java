package com.froloapp.telegramchart;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;
import com.froloapp.telegramchart.widget.linechartview.LineChartSlider;
import com.froloapp.telegramchart.widget.linechartview.LineChartView;

import java.util.Calendar;

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

        // try to reuse previously shown popup
        PopupWindow popupWindow;

        LineChartAdapter adapter;

        ViewHolder(View view) {
            rootView = view;
            chartView = view.findViewById(R.id.chartView);
            chartSlider = view.findViewById(R.id.chartSlider);
            layoutCheckboxes = view.findViewById(R.id.layoutCheckboxes);
        }

        void bind(LineChartAdapter adapter) {
            this.adapter = adapter;
            initChart(adapter);
            initCheckboxes(adapter);
        }

        private void initChart(LineChartAdapter adapter) {
            chartView.setOnStampClickListener(null);
            chartSlider.setOnScrollListener(null);

            final float startXPosition = 0.0f;
            final float stopXPosition = 0.3f;

            chartView.setOnStampClickListener(this);
            chartView.setAdapter(adapter, false);
            chartView.setXPositions(startXPosition, stopXPosition, false);

            chartSlider.setAdapter(adapter, false);
            chartSlider.setXPositions(startXPosition, stopXPosition, false);
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
                PopupWindow newWindow = getPopupWindow(timestamp);
                if (newWindow != null) {
                    newWindow.showAtLocation(chartView, Gravity.TOP | Gravity.LEFT, locX, locY);
                    this.popupWindow = newWindow;
                }
            } else {
                final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
                final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
                View v = currPopup.getContentView();
                populateWindowView(v, timestamp);
                currPopup.update(locX, locY, w, h);
            }
        }

        private void populateWindowView(View view, long timestamp) {
            LineChartAdapter adapter = this.adapter;
            if (adapter == null) {
                return;
            }
            final Context context = view.getContext();
            int timestampIndex = adapter.getTimestampIndex(timestamp);

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timestamp);
            int month = c.get(Calendar.MONTH);
            int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            String title = Utils.getDayOfWeekString(dayOfWeek) + ", " + Utils.getMonthString(month) + ' ' + dayOfMonth;
            ((TextView) view.findViewById(R.id.textStamp)).setText(title);

            LinearLayout layoutValues = view.findViewById(R.id.layoutValues);
            int countDiff = adapter.getEnabledLineCount() - layoutValues.getChildCount();
            if (countDiff > 0) {
                while (countDiff > 0) {
                    final int hp = (int) Utils.dpToPx(2f, context);
                    final int vp = (int) Utils.dpToPx(2f, context);
                    TextView textView = new TextView(context);
                    textView.setPadding(hp, vp, hp, vp);
                    layoutValues.addView(textView);
                    countDiff--;
                }
            } else if (countDiff < 0) {
                while (countDiff < 0) {
                    layoutValues.removeViewAt(layoutValues.getChildCount() - 1);
                    countDiff++;
                }
            }

            int childIndex = 0;
            for (int i = 0; i < adapter.getLineCount(); i++) {
                Line line = adapter.getLineAt(i);
                if (adapter.isLineEnabled(line)) {
                    final String text = line.getName() + "\n" + line.getValueAt(timestampIndex);
                    TextView textView = (TextView) layoutValues.getChildAt(childIndex);
                    textView.setTextColor(line.getColor());
                    textView.setText(text);
                    childIndex++;
                }
            }
        }

        private PopupWindow getPopupWindow(long timestamp) {
            LineChartAdapter adapter = this.adapter;
            if (adapter == null) {
                // early return. Shouldn't happen
                return null;
            }

            final Context context = rootView.getContext();
            final View v = LayoutInflater.from(context).inflate(R.layout.dialog_stamp_info, null, false);
            populateWindowView(v, timestamp);

            final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
            final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
            final PopupWindow popUp = new PopupWindow(v, w, h);

            popUp.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_stamp_info));
            popUp.setTouchable(false);
            popUp.setFocusable(true);
            popUp.setOutsideTouchable(true);
            return popUp;
        }
    }
}
