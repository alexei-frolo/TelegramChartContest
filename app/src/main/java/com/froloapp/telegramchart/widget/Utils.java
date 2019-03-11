package com.froloapp.telegramchart.widget;


import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class Utils {
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
}
