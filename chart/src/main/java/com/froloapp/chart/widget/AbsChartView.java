package com.froloapp.chart.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;


abstract class AbsChartView extends View {

    private static final int DEFAULT_WIDTH_IN_DP = 200;
    private static final int DEFAULT_HEIGHT_IN_DP = 100;

    public interface OnLineVisibilityChangedListener {
        void onLineVisibilityChanged(Line line, boolean isVisible);
    }

    private final ChartDelegate mChartDelegate = new ChartDelegate(this);

    private int mFooterHeight; // for X axis

    private Chart mChart;

    private OnLineVisibilityChangedListener mOnLineVisibilityChangedListener;

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
        mChartDelegate.loadAttributes(context, attrs);
        mChartDelegate.setXPositions(0.0f, 0.3f, false);
    }

    protected final ChartDelegate getChartDelegate() {
        return mChartDelegate;
    }

    public void setOnLineVisibilityChangedListener(OnLineVisibilityChangedListener l) {
        this.mOnLineVisibilityChangedListener = l;
    }

    private void dispatchLineVisibilityChanged(Line line, boolean isVisible) {
        if (mOnLineVisibilityChangedListener != null) {
            mOnLineVisibilityChangedListener.onLineVisibilityChanged(line, isVisible);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mChartDelegate.attach();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defWidth = (int) Misc.dpToPx(DEFAULT_WIDTH_IN_DP, getContext());
        final int defHeight = (int) Misc.dpToPx(DEFAULT_HEIGHT_IN_DP, getContext());
        final int measuredWidth = resolveSizeAndState(defWidth, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(defHeight, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);

        // dispatch measured
        mChartDelegate.measured();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mChartDelegate.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Reset the helper here
        mChartDelegate.detach();
    }

    protected final void setWillDrawXAxis(boolean willDraw) {
        mChartDelegate.setWillDrawXAxis(willDraw);
    }

    protected final void setWillDrawYAxis(boolean willDraw) {
        mChartDelegate.setWillDrawYAxis(willDraw);
    }

    void setFooterHeight(int height) {
        mFooterHeight = height;
        invalidate();
    }

    /* Nullable */
    public Chart getChart() {
        return mChart;
    }

    public void setChart(Chart chart, boolean animate) {
        mChart = chart;
        mChartDelegate.setChart(chart.getPoints(), chart.getLines(), animate);
    }

    public void setXPositions(float startXPosition, float stopXPosition, boolean animate) {
        mChartDelegate.setXPositions(startXPosition, stopXPosition, animate);
    }

    public boolean isLineVisible(Line line) {
        return mChartDelegate.isLineVisible(line);
    }

    public int getVisibleLineCount() {
        return mChartDelegate.getVisibleLineCount();
    }

    public void show(Line line, boolean animate) {
        mChartDelegate.show(line, animate);
        dispatchLineVisibilityChanged(line, true);
    }

    public void hide(Line line, boolean animate) {
        mChartDelegate.hide(line, animate);
        dispatchLineVisibilityChanged(line, false);
    }

    /* package */ int getFooterHeight() {
        return mFooterHeight;
    }

    static class SavedState extends BaseSavedState {
        private float mStartXPosition;
        private float mStopXPosition;
        private int[] mLineVisibilities;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            mStartXPosition = in.readFloat();
            mStopXPosition = in.readFloat();
            mLineVisibilities = in.createIntArray();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(mStartXPosition);
            out.writeFloat(mStopXPosition);
            out.writeIntArray(mLineVisibilities);
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

        ss.mStartXPosition = mChartDelegate.getStartXPosition();
        ss.mStopXPosition = mChartDelegate.getStopXPosition();

        int[] lineVisibilities = new int[mChartDelegate.getLineCount()];

        for (int i = 0; i < lineVisibilities.length; i++) {
            Line line = mChartDelegate.getLineAt(i);
            boolean isVisible = mChartDelegate.isLineVisible(line);
            lineVisibilities[i] = isVisible ? 1 : 0;
        }

        ss.mLineVisibilities = lineVisibilities;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        float startXPercentage = ss.mStartXPosition;
        float stopXPercentage = ss.mStopXPosition;
        int[] lineVisibilities = ss.mLineVisibilities;

        mChartDelegate.setXPositions(startXPercentage, stopXPercentage, false);

        if (lineVisibilities.length == mChartDelegate.getLineCount()) {
            for (int i = 0; i < lineVisibilities.length; i++) {
                boolean isVisible = lineVisibilities[i] > 0;
                Line line = mChartDelegate.getLineAt(i);
                if (isVisible) {
                    mChartDelegate.show(line, false);
                } else {
                    mChartDelegate.hide(line, false);
                }

                dispatchLineVisibilityChanged(line, isVisible);
            }
        }

    }
}
