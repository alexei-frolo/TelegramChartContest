package com.froloapp.chart.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.froloapp.chart.R;


public class ChartSlider extends AbsChartView {
    // static
    private static final int DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP = 5;
    private static final int DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP = 1;
    private static final float DEFAULT_MAX_FRAME_COMPRESSION = 0.2f;

    /**
     * The Slider is not currently scrolling.
     */
    public static final int SCROLL_STATE_IDLE = 0;
    /**
     * The Slider is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_FRAME_DRAGGING = 1;
    /**
     * The Slider frame left border is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_LEFT_BORDER_DRAGGING = 2;
    /**
     * The Slider frame right border is currently being dragged by outside input such as user touch input.
     */
    public static final int SCROLL_STATE_RIGHT_BORDER_DRAGGING = 3;
    /**
     * The Slider is currently animating to a final position while not under
     * outside control.
     */
    public static final int SCROLL_STATE_SETTLING = 4;

    private int scrollState = SCROLL_STATE_IDLE;
    private float mDragX = 0f;
    // If a finger touches a border in a place +- this threshold then the border must be under drag
    private float mTouchBorderThreshold;

    private final Paint mOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mFrameHorizontalBorderWidth;
    private float mFrameVerticalBorderWidth;

    private float mLeftBorderXPosition = 0f;
    private float mRightBorderXPosition = 1f;

    private float mMaxFrameCompression = 0.5f;

    // SCROLL LISTENER
    private OnScrollListener mListener;

    public ChartSlider(Context context) {
        this(context, null);
    }

    public ChartSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int overlayColor;
        int frameBorderColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.ChartSlider, 0, 0);

            overlayColor = typedArray.getColor(R.styleable.ChartSlider_overlayColor,
                    Color.parseColor("#AAFFFFFF"));

            frameBorderColor = typedArray.getColor(R.styleable.ChartSlider_frameBorderColor,
                    Color.parseColor("#AAC1C1C1"));

            mFrameHorizontalBorderWidth = typedArray.getDimension(R.styleable.ChartSlider_frameHorizontalBorderWidth,
                    Misc.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context));

            mFrameVerticalBorderWidth = typedArray.getDimension(R.styleable.ChartSlider_frameVerticalBorderWidth,
                    Misc.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context));

            mMaxFrameCompression = typedArray.getFloat(R.styleable.ChartSlider_maxFrameCompression,
                    DEFAULT_MAX_FRAME_COMPRESSION);

            typedArray.recycle();
        } else {
            overlayColor = Color.parseColor("#AAFFFFFF");
            frameBorderColor = Color.parseColor("#AAC1C1C1");
            mFrameHorizontalBorderWidth = Misc.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context);
            mFrameVerticalBorderWidth = Misc.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context);
            mMaxFrameCompression = DEFAULT_MAX_FRAME_COMPRESSION;
        }
        mOverlayPaint.setStyle(Paint.Style.FILL);
        mOverlayPaint.setColor(overlayColor);

        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setColor(frameBorderColor);

        mTouchBorderThreshold = Misc.dpToPx(5f, context);

        setWillDrawXAxis(false);
        setWillDrawYAxis(false);
    }

    public interface OnScrollListener {
        void onScroll(ChartSlider slider, float startStampRel, float endStampRel);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.mListener = listener;
    }

    private void dispatchScrolled(float startXPosition, float stopXPosition) {
        OnScrollListener l = mListener;
        if (l != null) {
            l.onScroll(this, startXPosition, stopXPosition);
        }
    }

    private float checkPercentage(float value) {
        if (value < 0)
            return 0;
        if (value > 1)
            return 1;
        return value;
    }

    private int getViewContentWith() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    private boolean isFrameLeftBorderTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * mLeftBorderXPosition;
        return x > startStampPos - mFrameHorizontalBorderWidth - mTouchBorderThreshold
                && x < startStampPos + mFrameHorizontalBorderWidth + mTouchBorderThreshold;
    }

    private boolean isFrameRightBorderTouched(float x) {
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * mRightBorderXPosition;
        return x > endStampPos - mFrameHorizontalBorderWidth - mTouchBorderThreshold
                && x < endStampPos + mFrameHorizontalBorderWidth + mTouchBorderThreshold;
    }

    private boolean isFrameTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * mLeftBorderXPosition;
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * mRightBorderXPosition;
        return x > startStampPos + mTouchBorderThreshold && x < endStampPos - mTouchBorderThreshold;
    }

    private boolean canCompressFrame(float startXPosition, float stopXPosition) {
        return stopXPosition - startXPosition >= mMaxFrameCompression;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getMeasuredWidth() - getPaddingRight();
        float bottom = getMeasuredHeight() - getPaddingBottom();

        float leftBorder = getPaddingLeft() + width * mLeftBorderXPosition;
        float rightBorder = getPaddingLeft() + width * mRightBorderXPosition;

        drawFrame(canvas, leftBorder, rightBorder);
        super.onDraw(canvas);
        drawOverlay(canvas, left, top, right, bottom, leftBorder, rightBorder);
    }

    private void drawOverlay(Canvas canvas, float left, float top, float right, float bottom, float leftBorder, float rightBorder) {
        // drawing left overlay
        canvas.drawRect(left, top, leftBorder, bottom, mOverlayPaint);
        // drawing right overlay
        canvas.drawRect(rightBorder, top, right, bottom, mOverlayPaint);
    }

    private void drawFrame(Canvas canvas, float leftBorder, float rightBorder) {
        mFramePaint.setStrokeWidth(mFrameHorizontalBorderWidth);
        // Left border
        canvas.drawLine(leftBorder + mFrameHorizontalBorderWidth / 2, getPaddingTop(),
                leftBorder + mFrameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), mFramePaint);
        // Right border
        canvas.drawLine(rightBorder - mFrameHorizontalBorderWidth / 2, getPaddingTop(),
                rightBorder - mFrameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), mFramePaint);

        mFramePaint.setStrokeWidth(mFrameVerticalBorderWidth);
        // Top border
        canvas.drawLine(leftBorder, getPaddingTop() + mFrameVerticalBorderWidth / 2,
                rightBorder, getPaddingTop() + mFrameVerticalBorderWidth / 2, mFramePaint);
        // Bottom border
        canvas.drawLine(leftBorder, getMeasuredHeight() - getPaddingBottom() - mFrameVerticalBorderWidth / 2,
                rightBorder, getMeasuredHeight() - getPaddingBottom() - mFrameVerticalBorderWidth / 2, mFramePaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                 // Detect if user starts dragging frame or one of frame borders
                float x = event.getX();
                mDragX = x;
                if (isFrameLeftBorderTouched(x)) {
                    scrollState = SCROLL_STATE_LEFT_BORDER_DRAGGING;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                } else if (isFrameRightBorderTouched(x)) {
                    scrollState = SCROLL_STATE_RIGHT_BORDER_DRAGGING;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                } else if (isFrameTouched(x)) {
                    scrollState = SCROLL_STATE_FRAME_DRAGGING;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                } else return super.onTouchEvent(event);
            }
            case MotionEvent.ACTION_MOVE: {
                if (scrollState == SCROLL_STATE_LEFT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - mDragX) / getViewContentWith();
                    mDragX = x;
                    float newStartXPosition = checkPercentage(mLeftBorderXPosition + frameScrollRel);
                    if (canCompressFrame(newStartXPosition, mRightBorderXPosition)) {
                        mLeftBorderXPosition = newStartXPosition;
                        dispatchScrolled(mLeftBorderXPosition, mRightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_RIGHT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - mDragX) / getViewContentWith();
                    mDragX = x;
                    float newStopXPosition = checkPercentage(mRightBorderXPosition + frameScrollRel);
                    if (canCompressFrame(mLeftBorderXPosition, newStopXPosition)) {
                        mRightBorderXPosition = newStopXPosition;
                        dispatchScrolled(mLeftBorderXPosition, mRightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_FRAME_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - mDragX) / getViewContentWith();
                    if (frameScrollRel > 0) {
                        frameScrollRel = Math.min(1 - mRightBorderXPosition, frameScrollRel);
                    } else {
                        frameScrollRel = -Math.min(mLeftBorderXPosition, -frameScrollRel);
                    }
                    mLeftBorderXPosition = checkPercentage(mLeftBorderXPosition + frameScrollRel);
                    mRightBorderXPosition = checkPercentage(mRightBorderXPosition + frameScrollRel);
                    mDragX = x;
                    dispatchScrolled(mLeftBorderXPosition, mRightBorderXPosition);
                    invalidate();
                    return true;
                }
                return super.onTouchEvent(event);
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                scrollState = SCROLL_STATE_IDLE;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setXPositions(float start, float stop, boolean animate) {
        this.mLeftBorderXPosition = start;
        this.mRightBorderXPosition = stop;
        super.setXPositions(0f, 1f, animate);
    }

    static class SavedState extends AbsChartView.SavedState {
        float leftBorderXPosition;
        float rightBorderXPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            leftBorderXPosition = in.readFloat();
            rightBorderXPosition = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(leftBorderXPosition);
            out.writeFloat(rightBorderXPosition);
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

        ss.leftBorderXPosition = mLeftBorderXPosition;
        ss.rightBorderXPosition = mRightBorderXPosition;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        mLeftBorderXPosition = ss.leftBorderXPosition;
        mRightBorderXPosition = ss.rightBorderXPosition;

        invalidate();
    }
}
