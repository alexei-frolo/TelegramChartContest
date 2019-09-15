package com.froloapp.telegramchart.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;


abstract class AbsChartView extends View {

    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;

    private final ChartHelper mChartHelper = new ChartHelper(this);

    private int mFooterHeight; // for X axis

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
        mChartHelper.loadAttributes(context, attrs);
        mChartHelper.setXPositions(0.0f, 0.3f, false);
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
        mFooterHeight = height;
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

    /* package */ int getFooterHeight() {
        return mFooterHeight;
    }

    static class SavedState extends BaseSavedState {
        private float mStartXPosition;
        private float mStopXPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            mStartXPosition = in.readFloat();
            mStopXPosition = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(mStartXPosition);
            out.writeFloat(mStopXPosition);
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

        ss.mStartXPosition = mChartHelper.getStartXPosition();
        ss.mStopXPosition = mChartHelper.getStopXPosition();

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        float startXPercentage = ss.mStartXPosition;
        float stopXPercentage = ss.mStopXPosition;

        mChartHelper.setXPositions(startXPercentage, stopXPercentage, false);

    }
}
