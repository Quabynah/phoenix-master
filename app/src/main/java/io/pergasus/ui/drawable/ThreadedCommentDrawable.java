/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

/**
 * A drawable showing the depth of a threaded conversation
 */
public class ThreadedCommentDrawable extends Drawable {

    @ColorInt
    private static final int THREAD_COLOR = 0xffeceef1;

    private final int threadWidth;
    private final int gap;
    private final int halfThreadWidth;
    private final Paint paint;
    private int threads;

    /**
     *
     * @param threadWidth in pixels
     * @param gap in pixels
     */
    public ThreadedCommentDrawable(int threadWidth, int gap) {
        this.threadWidth = threadWidth;
        this.gap = gap;
        halfThreadWidth = threadWidth / 2;
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(threadWidth);
        paint.setColor(THREAD_COLOR);
    }

    public ThreadedCommentDrawable(int threadWidth, int gap, int depth) {
        this(threadWidth, gap);
        setDepth(depth);
    }

    public void setDepth(int depth) {
        this.threads = depth + 1;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        for (int thread = 0; thread < threads; thread++) {
            int left = halfThreadWidth + (thread * (threadWidth + gap));
            canvas.drawLine(left, 0, left, getBounds().bottom, paint);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return (threads * threadWidth) + ((threads - 1) * gap);
    }

    @Override
    public void setAlpha(int i) {
        paint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return paint.getAlpha() == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadedCommentDrawable that = (ThreadedCommentDrawable) o;
        return threads == that.threads;
    }

    @Override
    public int hashCode() {
        return threads;
    }
}
