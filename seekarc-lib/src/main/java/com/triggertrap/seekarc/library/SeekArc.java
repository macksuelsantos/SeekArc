/*******************************************************************************
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2013 Triggertrap Ltd
 * Author Neil Davies
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.triggertrap.seekarc.library;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * SeekArc.java
 * <p>
 * This is a class that functions much like a SeekBar but
 * follows a circle path instead of a straight line.
 *
 * @author Neil Davies
 */
public class SeekArc extends View implements ValueAnimator.AnimatorUpdateListener {

    private static final String TAG = SeekArc.class.getSimpleName();

    private SeekArcColor mSeekArcColor = new SeekArcColor(Color.WHITE, Color.BLACK);

    /**
     * The Drawable for the seek arc thumbnail
     */
    private Drawable mThumb;

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private int mMax = 100;

    /**
     * The Current value that the SeekArc is set to
     */
    private int mProgress = 0;

    /**
     * The Width of the background arc for the SeekArc
     */
    private int mArcWidth = 8;

    /**
     * The Angle to start drawing this Arc from
     */
    private int mStartAngle = 45;

    /**
     * The Angle through which to draw the arc (Max is 360)
     */
    private int mSweepAngle = 270;

    /**
     * The rotation of the SeekArc- 0 is twelve o'clock
     */
    private int mRotation = 180;

    private int mProjectScoreSize = 25;

    /**
     * Give the SeekArc rounded edges
     */
    private boolean mRoundedEdges = false;

    private boolean mProjectedScore = true;

    private boolean mHasPending = false;

    private boolean mIsSample = false;

    private boolean mIsCEFR = false;

    private String bottomTextTitle = "SESSION";
    private String bottomTextSubTitle = "SCORE";
    /**
     * Will the progress increase clockwise or anti-clockwise
     */
    private boolean mClockwise = true;

    private ObjectAnimator mAnimator;

    // Internal variables
    private int mArcRadius = 0;
    private float mProgressSweep = 0;
    private RectF mArcRect = new RectF();
    private Paint mArcPaint;
    private Paint mProgressPaint;
    private Paint mTextPaint;
    private int mTranslateX;
    private int mTranslateY;
    private int mThumbXPos;
    private int mThumbYPos;
    private int mArcColor;
    private float arcBottomHeight;
    private OnSeekArcChangeListener mOnSeekArcChangeListener;
    private String mCEFRScore;

    public interface OnSeekArcChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param seekArc  The SeekArc whose progress has changed
         * @param progress The current progress level. This will be in the range
         *                 0..max where max was set by
         *                 {@link .ProgressArcsetMax(int)}. (The default value for
         *                 max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser);
    }

    public SeekArc(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SeekArc(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SeekArc(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        if (isInEditMode()) return;

        Log.d(TAG, "Initialising SeekArc");
        setArcColorGreen();
        int padding = (int) Utils.dp2px(getResources(), 30);
        setPadding(padding, padding, padding, padding);
        final Resources res = getResources();

        // Defaults, may need to link this into theme settings
        mArcColor = res.getColor(R.color.progress_gray);
        int thumbHalfheight;
        int thumbHalfWidth;
        mThumb = res.getDrawable(R.drawable.seek_arc_control_selector);

        if (attrs != null) {
            // Attribute initialization
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SeekArc, defStyle, 0);

            Drawable thumb = a.getDrawable(R.styleable.SeekArc_thumb);
            if (thumb != null) {
                mThumb = thumb;
            }

            thumbHalfheight = mThumb.getIntrinsicHeight() / 2;
            thumbHalfWidth = mThumb.getIntrinsicWidth() / 2;
            mThumb.setBounds(-thumbHalfWidth, -thumbHalfheight, thumbHalfWidth,
                    thumbHalfheight);

            mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
            mProgress = a.getInteger(R.styleable.SeekArc_progress, mProgress);
            mArcWidth = (int) a.getDimension(R.styleable.SeekArc_arcWidth, mArcWidth);
            mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges, mRoundedEdges);
            mClockwise = a.getBoolean(R.styleable.SeekArc_clockwise, mClockwise);

            mArcColor = a.getColor(R.styleable.SeekArc_arcColor, mArcColor);
            a.recycle();
        }

        mProgress = (mProgress > mMax) ? mMax : mProgress;
        mProgress = (mProgress < 0) ? 0 : mProgress;

        mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
        mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;

        mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
        mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;

        initPainters();
    }

    private void initPainters() {
        mTextPaint = new TextPaint();
        mTextPaint.setColor(Color.parseColor("#5d6670"));
        mTextPaint.setAntiAlias(true);

        mArcPaint = new Paint();
        mArcPaint.setColor(mArcColor);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        /*
      The width of the progress line for this SeekArc
	 */
        int mProgressWidth = (int) Utils.dp2px(getResources(), 8);
        mProgressPaint.setStrokeWidth(mProgressWidth);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setShader(new LinearGradient(0, 0, getWidth(), 0, mSeekArcColor.colorEnd, mSeekArcColor.colorIn, Shader.TileMode.MIRROR));

        if (mRoundedEdges) {
            mArcPaint.setStrokeCap(Paint.Cap.ROUND);
            mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mClockwise) {
            canvas.scale(-1, 1, mArcRect.centerX(), mArcRect.centerY());
        }

        if (!isInEditMode()) {
            LinearGradient linearGradient = new LinearGradient(0, 0, getWidth(), 0, mSeekArcColor.colorIn, mSeekArcColor.colorEnd, Shader.TileMode.MIRROR);
            mProgressPaint.setShader(linearGradient);

            onDrawProgress(canvas);
            onDrawTextsButton(canvas);
            onDrawArc(canvas);
        }
    }

    private void onDrawArc(Canvas canvas) {
        int mAngleOffset = -90;
        final int arcStart = mStartAngle + mAngleOffset + mRotation;
        final int arcSweep = mSweepAngle;
        canvas.drawArc(mArcRect, arcStart, arcSweep, false, mArcPaint);

        canvas.drawArc(mArcRect, arcStart, mProgressSweep, false, mProgressPaint);

        // Draw the thumb nail
        canvas.translate(mTranslateX - mThumbXPos, mTranslateY - mThumbYPos);
        mThumb.draw(canvas);
    }

    private void onDrawProgress(Canvas canvas) {
        if (!mIsSample && !mHasPending) {
            String text;

            if (mIsCEFR) {
                text = mCEFRScore;
            } else if (mProjectedScore) {
                text = String.valueOf(mProgress) + "/" + mMax;
            } else {
                int value = Math.round(((float) mProgress / (float) mMax) * 100);
                text = value + "%";
            }

            if (!TextUtils.isEmpty(text)) {
                mTextPaint.setTextSize(Utils.dp2px(getResources(), mProjectScoreSize));
                float textHeight = mTextPaint.descent() + mTextPaint.ascent();
                float textBaseline = (getHeight() - textHeight) / 2f;

                canvas.drawText(text, (getWidth() - mTextPaint.measureText(text)) / 2f, textBaseline, mTextPaint);
            }
        }
    }

    private void onDrawTextsButton(Canvas canvas) {
        float x, y;

        mTextPaint.setTextSize(Utils.dp2px(getResources(), 9));
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        x = (getWidth() - mTextPaint.measureText(bottomTextTitle)) / 2f;
        y = (getHeight() / 2) + (arcBottomHeight / 2) - getPaddingBottom();
        canvas.drawText(bottomTextTitle, x, y, mTextPaint);

        if (!bottomTextSubTitle.isEmpty()) {
            mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            x = (getWidth() - mTextPaint.measureText(bottomTextSubTitle)) / 2f;
            y += mTextPaint.descent() - mTextPaint.ascent();
            canvas.drawText(bottomTextSubTitle, x, y, mTextPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        final int min = Math.min(width, height);
        float top;
        float left;
        int arcDiameter;

        mTranslateX = (int) (width * 0.5f);
        mTranslateY = (int) (height * 0.5f);

        arcDiameter = min - getPaddingLeft();
        mArcRadius = arcDiameter / 2;
        top = height / 2 - (arcDiameter / 2);
        left = width / 2 - (arcDiameter / 2);
        mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);

        int arcStart = (int) mProgressSweep + mStartAngle + mRotation + 90;
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));

        float radius = width / 2f;
        float angle = (360 - mStartAngle) / 2f;
        arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mThumb != null && mThumb.isStateful()) {
            int[] state = getDrawableState();
            mThumb.setState(state);
        }
        invalidate();
    }

    private void updateThumbPosition() {
        int thumbAngle = (int) (mStartAngle + mProgressSweep + mRotation + 90);
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(thumbAngle)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(thumbAngle)));
    }

    private void updateProgress(int progress, boolean fromUser) {
        if (isInEditMode()) return;

        int INVALID_PROGRESS_VALUE = -1;
        if (progress == INVALID_PROGRESS_VALUE) {
            return;
        }

        if (mOnSeekArcChangeListener != null) {
            mOnSeekArcChangeListener
                    .onProgressChanged(this, progress, fromUser);
        }

        progress = (progress > mMax) ? mMax : progress;
        progress = (mProgress < 0) ? 0 : progress;

        mProgress = progress;
        mProgressSweep = (float) progress / mMax * mSweepAngle;

        updateThumbPosition();

        invalidate();
    }

    public void setProjectedScore(boolean isProjectedScore, String title, String subtitle) {
        this.bottomTextTitle = title;
        this.bottomTextSubTitle = subtitle;
        this.mProjectedScore = isProjectedScore;
        invalidate();
    }

    public void setHasPeding(String title, String subtitle) {
        this.bottomTextTitle = title;
        this.bottomTextSubTitle = subtitle;
        this.mHasPending = true;
        invalidate();
    }

    public void setSample(String title, String subtitle) {
        this.mIsSample = true;
        this.bottomTextTitle = title;
        this.bottomTextSubTitle = subtitle;

        setProgressAnimate(0);
        invalidate();
    }

    public void setIsCEFR(String cefrScore, String title, String subtitle) {
        this.bottomTextTitle = title;
        this.bottomTextSubTitle = subtitle;
        this.mIsCEFR = true;
        this.mCEFRScore = cefrScore;
    }

    public void setArcColorGreen() {
        this.mSeekArcColor = new SeekArcColor(Color.parseColor("#08b267"), Color.parseColor("#1cdfc0"));
        invalidate();
    }

    public void setArcColorRed() {
        this.mSeekArcColor = new SeekArcColor(Color.parseColor("#b80b0b"), Color.parseColor("#ff5252"));
        invalidate();
    }

    public void setArcColorYellow() {
        this.mSeekArcColor = new SeekArcColor(Color.parseColor("#fac612"), Color.parseColor("#dbae11"));
        invalidate();
    }

    public void setMax(int max) {
        this.mMax = max;
        invalidate();
    }

    public void setTextColor(int color) {
        this.mTextPaint.setColor(color);
        invalidate();
    }

    public void setProjectScoreSize(int textSize) {
        this.mProjectScoreSize = textSize;
        invalidate();
    }

    public synchronized void setProgressAnimate(int progress) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {

            if (mAnimator != null) {
                mAnimator.cancel();
            }

            mAnimator = ObjectAnimator.ofInt(SeekArc.this, "progress", 0, progress);
            mAnimator.addUpdateListener(SeekArc.this);
            mAnimator.setDuration(30 * progress);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.start();
        }
    }

    public synchronized void setPercentAnimate(float percent) {
        int progress = Math.round(((float) mMax * (float) percent) / 100);
        setProgressAnimate(progress);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        int progress = (int) animation.getAnimatedValue();
        this.updateProgress(progress, false);
    }

    public static class SeekArcColor {
        public int colorIn, colorEnd;

        public SeekArcColor(int colorIn, int colorEnd) {
            this.colorIn = colorIn;
            this.colorEnd = colorEnd;
        }
    }
}
