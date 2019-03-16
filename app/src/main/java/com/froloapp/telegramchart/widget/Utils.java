package com.froloapp.telegramchart.widget;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

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
}
