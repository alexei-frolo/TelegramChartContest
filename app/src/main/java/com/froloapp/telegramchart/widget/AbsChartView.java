package com.froloapp.telegramchart.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.froloapp.telegramchart.R;


public class AbsChartView extends View {

    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;

    private static final float DEFAULT_CHART_LINE_WIDTH_IN_DP = 1f;
    private static final float DEFAULT_AXIS_LINE_WIDTH_IN_DP = 1f;

    private final ChartHelper mChartHelper = new ChartHelper(this);

    private int footerHeight; // for x axis stamps

    public AbsChartView(Context context) {
        this(context, null);
    }

    public AbsChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float chartLineWidth;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AbsLineChartView, 0, 0);
            chartLineWidth = typedArray.getDimension(R.styleable.AbsLineChartView_chartStrokeWidth, Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context));
            //axisStrokeWidth = typedArray.getDimension(R.styleable.AbsLineChartView_axisStrokeWidth, Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context));
            typedArray.recycle();
        } else {
            chartLineWidth = Utils.dpToPx(DEFAULT_CHART_LINE_WIDTH_IN_DP, context);
            //axisStrokeWidth = Utils.dpToPx(DEFAULT_AXIS_LINE_WIDTH_IN_DP, context);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mChartHelper.attach();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Utils.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Utils.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);

        // dispatch measured
        mChartHelper.measured();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mChartHelper.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Reset the helper here
        mChartHelper.detach();
    }

    protected final void setWillDrawXAxis(boolean willDraw) {
        mChartHelper.setWillDrawXAxis(willDraw);
    }

    protected final void setWillDrawYAxis(boolean willDraw) {
        mChartHelper.setWillDrawYAxis(willDraw);
    }

    void setFooterHeight(int height) {
        footerHeight = height;
        invalidate();
    }

    /* *********************************
     ********* PUBLIC INTERFACE ********
     **********************************/

    public void setChart(Chart chart, boolean animate) {
        mChartHelper.setChart(chart.getPoints(), chart.getLines(), animate);
    }

    public void setXPositions(float startXPosition, float stopXPosition, boolean animate) {
        mChartHelper.setXPositions(startXPosition, stopXPosition, animate);
    }

    public boolean isLineVisible(Line line) {
        return mChartHelper.isLineVisible(line);
    }

    public void show(Line line, boolean animate) {
        mChartHelper.show(line, animate);
    }

    public void hide(Line line, boolean animate) {
        mChartHelper.hide(line, animate);
    }

    /* *********************************
     ********* PACKAGE INTERFACE *******
     **********************************/

    /*abstract*/ int getFooterHeight() {
        return footerHeight;
    }

    /* *********************************
     ****** SAVING INSTANCE STATE ******
     **********************************/

    static class SavedState extends BaseSavedState {
        private float startXPercentage;
        private float stopXPercentage;
        private float minYValue;
        private float maxYValue;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            startXPercentage = in.readFloat();
            stopXPercentage = in.readFloat();
            minYValue = in.readFloat();
            maxYValue = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(startXPercentage);
            out.writeFloat(stopXPercentage);
            out.writeFloat(minYValue);
            out.writeFloat(maxYValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

//        ss.startXPercentage = startXPercentage;
//        ss.stopXPercentage = stopXPercentage;
//        ss.minYValue = minYValue;
//        ss.maxYValue = maxYValue;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        float startXPercentage = ss.startXPercentage;
        float stopXPercentage = ss.stopXPercentage;
        float minYValue = ss.minYValue;
        float maxYValue = ss.maxYValue;

    }
}
