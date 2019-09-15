package com.froloapp.chart;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.froloapp.chart.widget.Chart;
import com.froloapp.chart.widget.ChartView;
import com.froloapp.chart.widget.Line;
import com.froloapp.chart.widget.Misc;

import java.util.Calendar;

final class PopupHelper {
    private PopupHelper() { }

    static void populateWindowView(View view, ChartView chartView, int stampIndex) {
        Chart chart = chartView.getChart();

        if (chart == null) {
            return;
        }

        final Context context = view.getContext();

        long stamp = chart.getStampAt(stampIndex);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(stamp);
        int month = c.get(Calendar.MONTH);
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        String title = Utils.getDayOfWeekString(dayOfWeek) + ", " + Utils.getMonthString(month) + ' ' + dayOfMonth;
        ((TextView) view.findViewById(R.id.textStamp)).setText(title);


        LinearLayout layoutValues = view.findViewById(R.id.layoutValues);
        int countDiff = chartView.getVisibleLineCount() - layoutValues.getChildCount();
        if (countDiff > 0) {
            while (countDiff > 0) {
                final int hp = (int) Misc.dpToPx(2f, context);
                final int vp = (int) Misc.dpToPx(2f, context);
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
        for (int i = 0; i < chart.getLineCount(); i++) {
            Line line = chart.getLineAt(i);
            if (chartView.isLineVisible(line)) {
                final String text = line.getName() + "\n" + line.getValueAt(stampIndex);
                TextView textView = (TextView) layoutValues.getChildAt(childIndex);
                textView.setTextColor(line.getColor());
                textView.setText(text);
                childIndex++;
            }
        }
    }

    static /*nullable*/ PopupWindow createPopupWindow(ChartView view, int stampIndex) {
        Chart chart = view.getChart();
        if (chart == null) {
            // early return. Shouldn't happen
            return null;
        }

        final Context context = view.getContext();
        final View popupView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.dialog_stamp_info, null, false);
        populateWindowView(popupView, view, stampIndex);

        final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        final PopupWindow popUp = new PopupWindow(popupView, w, h);

        popUp.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_stamp_info));
        popUp.setTouchable(false);
        popUp.setFocusable(true);
        popUp.setOutsideTouchable(true);

        return popUp;
    }
}
