package com.froloapp.chart.example;

import android.graphics.Rect;
import android.view.View;

import java.util.Calendar;

class Utils {
    private Utils() {
    }

    static String getTextForTimestamp(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        return Utils.getMonthString(month) + ' ' + day;
    }

    static String getMonthString(int month) {
        switch (month) {
            case 0: return "Jan";
            case 1: return "Feb";
            case 2: return "Mar";
            case 3: return "Apr";
            case 4: return "May";
            case 5: return "Jun";
            case 6: return "Jul";
            case 7: return "Aug";
            case 8: return "Sep";
            case 9: return "Oct";
            case 10: return "Nov";
            case 11: return "Dec";
            default: return null;
        }
    }

    static String getDayOfWeekString(int dayOfWeek) {
        switch (dayOfWeek) {
            case 0: return "Mon";
            case 1: return "Tue";
            case 2: return "Wed";
            case 3: return "Apr";
            case 4: return "Thu";
            case 5: return "Fri";
            case 6: return "Sat";
            case 7: return "Sun";
            default: return null;
        }
    }

    public static /*nullable*/ Rect getViewLocation(View v) {
        int[] loc = new int[2];
        try {
            v.getLocationOnScreen(loc);
        } catch (NullPointerException ignored) { // seems the view is not attached to window anymore
            return null;
        }
        Rect location = new Rect();
        location.left = loc[0];
        location.top = loc[1];
        location.right = location.left + v.getWidth();
        location.bottom = location.top + v.getHeight();
        return location;
    }
}
