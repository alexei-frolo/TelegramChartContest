package com.froloapp.chart.widget;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public final class Misc {
    private static final NavigableMap<Integer, String> BIG_NUMBER_SUFFIXES = new TreeMap<>();
    static {
        BIG_NUMBER_SUFFIXES.put(1_000, "k");
        BIG_NUMBER_SUFFIXES.put(1_000_000, "M");
        BIG_NUMBER_SUFFIXES.put(1_000_000_000, "G");
    }

    private Misc() {
    }

    public static float dpToPx(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float pxToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static void defineTextSize(Paint paint, float desiredHeight, String text) {
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float desiredTextSize = testTextSize * desiredHeight / bounds.height();
        paint.setTextSize(desiredTextSize);
    }

    static String format(int value) {
        if (value == Integer.MIN_VALUE) return format(Integer.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Integer.toString(value); //deal with easy case

        Map.Entry<Integer, String> e = BIG_NUMBER_SUFFIXES.floorEntry(value);
        int divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}
