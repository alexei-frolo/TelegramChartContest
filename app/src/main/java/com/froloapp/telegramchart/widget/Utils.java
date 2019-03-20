package com.froloapp.telegramchart.widget;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class Utils {
    private static final NavigableMap<Long, String> BIG_NUMBER_SUFFIXES = new TreeMap<>();
    static {
        BIG_NUMBER_SUFFIXES.put(1_000L, "k");
        BIG_NUMBER_SUFFIXES.put(1_000_000L, "M");
        BIG_NUMBER_SUFFIXES.put(1_000_000_000L, "G");
        BIG_NUMBER_SUFFIXES.put(1_000_000_000_000L, "T");
        BIG_NUMBER_SUFFIXES.put(1_000_000_000_000_000L, "P");
        BIG_NUMBER_SUFFIXES.put(1_000_000_000_000_000_000L, "E");
    }

    private Utils() {
    }

    public static float dpToPx(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float pxToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static float checkPercentage(float p) {
        if (p < 0f || p > 1f) {
            throw new IllegalArgumentException("Invalid percentage: " + p);
        }
        return p;
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

    public static void defineTextSize(Paint paint, float desiredHeight, String text) {
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float desiredTextSize = testTextSize * desiredHeight / bounds.height();
        paint.setTextSize(desiredTextSize);
    }

    public static String getMonthString(int month) {
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

    public static String getDayOfWeekString(int dayOfWeek) {
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

    public static String format(long value) {
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = BIG_NUMBER_SUFFIXES.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}
