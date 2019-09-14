//package com.froloapp.telegramchart;
//
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import android.widget.PopupWindow;
//import android.widget.TextView;
//
//import com.froloapp.telegramchart.widget.Utils;
//
//import java.util.Calendar;
//
//final class PopupHelper {
//    private PopupHelper() { }
//
//    public static void populateWindowView(View view, LineChartAdapter adapter, long timestamp) {
//        if (adapter == null) {
//            return;
//        }
//        final Context context = view.getContext();
//        int timestampIndex = adapter.getTimestampIndex(timestamp);
//
//        Calendar c = Calendar.getInstance();
//        c.setTimeInMillis(timestamp);
//        int month = c.get(Calendar.MONTH);
//        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
//        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
//        String title = Utils.getDayOfWeekString(dayOfWeek) + ", " + Utils.getMonthString(month) + ' ' + dayOfMonth;
//        ((TextView) view.findViewById(R.id.textStamp)).setText(title);
//
//        LinearLayout layoutValues = view.findViewById(R.id.layoutValues);
//        int countDiff = adapter.getEnabledLineCount() - layoutValues.getChildCount();
//        if (countDiff > 0) {
//            while (countDiff > 0) {
//                final int hp = (int) Utils.dpToPx(2f, context);
//                final int vp = (int) Utils.dpToPx(2f, context);
//                TextView textView = new TextView(context);
//                textView.setPadding(hp, vp, hp, vp);
//                layoutValues.addView(textView);
//                countDiff--;
//            }
//        } else if (countDiff < 0) {
//            while (countDiff < 0) {
//                layoutValues.removeViewAt(layoutValues.getChildCount() - 1);
//                countDiff++;
//            }
//        }
//
//        int childIndex = 0;
//        for (int i = 0; i < adapter.getLineCount(); i++) {
//            OldLine line = adapter.getLineAt(i);
//            if (adapter.isLineEnabled(line)) {
//                final String text = line.getName() + "\n" + line.getValueAt(timestampIndex);
//                TextView textView = (TextView) layoutValues.getChildAt(childIndex);
//                textView.setTextColor(line.getColor());
//                textView.setText(text);
//                childIndex++;
//            }
//        }
//    }
//
//    public static /*nullable*/ PopupWindow createPopupWindow(Context context, LineChartAdapter adapter, long timestamp) {
//        if (adapter == null) {
//            // early return. Shouldn't happen
//            return null;
//        }
//
//        final View v = LayoutInflater.from(context).inflate(R.layout.dialog_stamp_info, null, false);
//        populateWindowView(v, adapter, timestamp);
//
//        final int w = ViewGroup.LayoutParams.WRAP_CONTENT;
//        final int h = ViewGroup.LayoutParams.WRAP_CONTENT;
//        final PopupWindow popUp = new PopupWindow(v, w, h);
//
//        popUp.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_stamp_info));
//        popUp.setTouchable(false);
//        popUp.setFocusable(true);
//        popUp.setOutsideTouchable(true);
//        return popUp;
//    }
//}
