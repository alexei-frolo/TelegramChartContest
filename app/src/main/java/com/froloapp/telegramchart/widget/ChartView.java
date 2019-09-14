package com.froloapp.telegramchart.widget;

import android.content.Context;
import android.util.AttributeSet;


public class ChartView extends AbsChartView {

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillDrawXAxis(true);
        setWillDrawYAxis(true);
    }
}
