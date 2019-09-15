package com.froloapp.telegramchart.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.froloapp.telegramchart.BuildConfig;


public class ChartView extends AbsChartView {

    private static final String LOG_TAG = "ChartView";

    public interface OnStampClickListener {
        void onTouchDown(ChartView view, int stampIndex, float stampXCoordinate);
        void onTouchUp(ChartView view);
    }

    private OnStampClickListener mOnStampClickListener;

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

    private void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg);
    }

    public void setOnStampClickListener(OnStampClickListener l) {
        this.mOnStampClickListener = l;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //wasClickedStamp = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);
                float x = event.getX();
                handleTouch(x);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                handleTouch(x);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                getChartDelegate().clearSelectedXPosition();
                getParent().requestDisallowInterceptTouchEvent(false);
                dispatchTouchUp();
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleTouch(float x) {
        ChartDelegate helper = getChartDelegate();
        float xPosition = CommonHelper.calcCoordinateRelativePosition(
                this,
                helper.getStartXPosition(),
                helper.getStopXPosition(),
                x);
        log("Touched: x_coor=" + x + ", x_pos=" + xPosition);
        helper.setSelectedXPosition(xPosition);

        int stampIndex = CommonHelper.getClosestPointIndex(
                helper.getPoints(),
                xPosition);

        dispatchTouchDown(stampIndex, x);
    }

    private void dispatchTouchDown(int stampIndex, float stampXCoordinate) {
        if (mOnStampClickListener != null) {
            mOnStampClickListener.onTouchDown(this, stampIndex, stampXCoordinate);
        }
    }

    private void dispatchTouchUp() {
        if (mOnStampClickListener != null) {
            mOnStampClickListener.onTouchUp(this);
        }
    }
}
