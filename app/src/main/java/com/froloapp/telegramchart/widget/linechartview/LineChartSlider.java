package com.froloapp.telegramchart.widget.linechartview;

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

import com.froloapp.telegramchart.R;
import com.froloapp.telegramchart.widget.Utils;

public class LineChartSlider extends AbsLineChartView {
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
    private float xDragPos = 0f;
    // If a finger touches a border in a place +- this threshold then the border must be under drag
    private float touchBorderThreshold;

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float frameHorizontalBorderWidth;
    private float frameVerticalBorderWidth;

    private float leftBorderXPosition = 0f;
    private float rightBorderXPosition = 1f;

    private float maxFrameCompression = 0.5f;

    // SCROLL LISTENER
    private OnScrollListener listener;

    public LineChartSlider(Context context) {
        this(context, null);
    }

    public LineChartSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int overlayColor;
        int frameBorderColor;
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LineChartSlider, 0, 0);
            overlayColor = typedArray.getColor(R.styleable.LineChartSlider_overlayColor, Color.parseColor("#AAFFFFFF"));
            frameBorderColor = typedArray.getColor(R.styleable.LineChartSlider_frameBorderColor, Color.parseColor("#AAC1C1C1"));
            frameHorizontalBorderWidth = typedArray.getDimension(R.styleable.LineChartSlider_frameHorizontalBorderWidth,
                    Utils.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context));
            frameVerticalBorderWidth = typedArray.getDimension(R.styleable.LineChartSlider_frameVerticalBorderWidth,
                    Utils.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context));
            maxFrameCompression = typedArray.getFloat(R.styleable.LineChartSlider_maxFrameCompression, DEFAULT_MAX_FRAME_COMPRESSION);
            typedArray.recycle();
        } else {
            overlayColor = Color.parseColor("#AAFFFFFF");
            frameBorderColor = Color.parseColor("#AAC1C1C1");
            frameHorizontalBorderWidth = Utils.dpToPx(DEFAULT_FRAME_HORIZONTAL_BORDER_WIDTH_IN_DP, context);
            frameVerticalBorderWidth = Utils.dpToPx(DEFAULT_FRAME_VERTICAl_BORDER_WIDTH_IN_DP, context);
            maxFrameCompression = DEFAULT_MAX_FRAME_COMPRESSION;
        }
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(overlayColor);

        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setColor(frameBorderColor);

        touchBorderThreshold = Utils.dpToPx(5f, context);
    }

    public interface OnScrollListener {
        void onScroll(LineChartSlider slider, float startStampRel, float endStampRel);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.listener = listener;
    }

    private void dispatchScrolled(float startXPosition, float stopXPosition) {
        OnScrollListener l = listener;
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
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * leftBorderXPosition;
        return x > startStampPos - frameHorizontalBorderWidth - touchBorderThreshold
                && x < startStampPos + frameHorizontalBorderWidth + touchBorderThreshold;
    }

    private boolean isFrameRightBorderTouched(float x) {
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * rightBorderXPosition;
        return x > endStampPos - frameHorizontalBorderWidth - touchBorderThreshold
                && x < endStampPos + frameHorizontalBorderWidth + touchBorderThreshold;
    }

    private boolean isFrameTouched(float x) {
        float startStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * leftBorderXPosition;
        float endStampPos = getPaddingLeft() + (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) * rightBorderXPosition;
        return x > startStampPos + touchBorderThreshold && x < endStampPos - touchBorderThreshold;
    }

    private boolean canCompressFrame(float startXPosition, float stopXPosition) {
        return stopXPosition - startXPosition >= maxFrameCompression;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getMeasuredWidth() - getPaddingRight();
        float bottom = getMeasuredHeight() - getPaddingBottom();

        float leftBorder = getPaddingLeft() + width * leftBorderXPosition;
        float rightBorder = getPaddingLeft() + width * rightBorderXPosition;

        drawFrame(canvas, leftBorder, rightBorder);
        drawLines(canvas);
        drawOverlay(canvas, left, top, right, bottom, leftBorder, rightBorder);
    }

    private void drawOverlay(Canvas canvas, float left, float top, float right, float bottom, float leftBorder, float rightBorder) {
        // drawing left overlay
        canvas.drawRect(left, top, leftBorder, bottom, overlayPaint);
        // drawing right overlay
        canvas.drawRect(rightBorder, top, right, bottom, overlayPaint);
    }

    private void drawFrame(Canvas canvas, float leftBorder, float rightBorder) {
        framePaint.setStrokeWidth(frameHorizontalBorderWidth);
        // Left border
        canvas.drawLine(leftBorder + frameHorizontalBorderWidth / 2, getPaddingTop(),
                leftBorder + frameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), framePaint);
        // Right border
        canvas.drawLine(rightBorder - frameHorizontalBorderWidth / 2, getPaddingTop(),
                rightBorder - frameHorizontalBorderWidth / 2, getMeasuredHeight() - getPaddingBottom(), framePaint);

        framePaint.setStrokeWidth(frameVerticalBorderWidth);
        // Top border
        canvas.drawLine(leftBorder, getPaddingTop() + frameVerticalBorderWidth / 2,
                rightBorder, getPaddingTop() + frameVerticalBorderWidth / 2, framePaint);
        // Bottom border
        canvas.drawLine(leftBorder, getMeasuredHeight() - getPaddingBottom() - frameVerticalBorderWidth / 2,
                rightBorder, getMeasuredHeight() - getPaddingBottom() - frameVerticalBorderWidth / 2, framePaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                 // Detect if user starts dragging frame or one of frame borders
                float x = event.getX();
                xDragPos = x;
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
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStartXPosition = checkPercentage(leftBorderXPosition + frameScrollRel);
                    if (canCompressFrame(newStartXPosition, rightBorderXPosition)) {
                        leftBorderXPosition = newStartXPosition;
                        dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_RIGHT_BORDER_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    xDragPos = x;
                    float newStopXPosition = checkPercentage(rightBorderXPosition + frameScrollRel);
                    if (canCompressFrame(leftBorderXPosition, newStopXPosition)) {
                        rightBorderXPosition = newStopXPosition;
                        dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
                        invalidate();
                    }
                    return true;
                } else if (scrollState == SCROLL_STATE_FRAME_DRAGGING) {
                    float x = event.getX();
                    float frameScrollRel = (x - xDragPos) / getViewContentWith();
                    if (frameScrollRel > 0) {
                        frameScrollRel = Math.min(1 - rightBorderXPosition, frameScrollRel);
                    } else {
                        frameScrollRel = -Math.min(leftBorderXPosition, -frameScrollRel);
                    }
                    leftBorderXPosition = checkPercentage(leftBorderXPosition + frameScrollRel);
                    rightBorderXPosition = checkPercentage(rightBorderXPosition + frameScrollRel);
                    xDragPos = x;
                    dispatchScrolled(leftBorderXPosition, rightBorderXPosition);
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
    public void setAdapter(LineChartAdapter adapter, boolean animate) {
        super.setAdapter(adapter, animate);
        super.setXPositions(0f, 1f, animate);
    }

    @Override
    public void setXPositions(float start, float stop, boolean animate) {
        this.leftBorderXPosition = start;
        this.rightBorderXPosition = stop;
        super.setXPositions(0f, 1f, animate);
    }

    /* *********************************
     ****** SAVING INSTANCE STATE ******
     **********************************/

    static class SavedState extends AbsLineChartView.SavedState {
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

        ss.leftBorderXPosition = leftBorderXPosition;
        ss.rightBorderXPosition = rightBorderXPosition;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        leftBorderXPosition = ss.leftBorderXPosition;
        rightBorderXPosition = ss.rightBorderXPosition;

        invalidate();
    }
}
