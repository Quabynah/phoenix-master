/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:34 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.util.Property;

import io.pergasus.R;
import io.pergasus.util.ColorUtils;

/**
 * An image view which supports parallax scrolling and applying a scrim onto it's content. Get it.
 * <p>
 * It also has a custom pinned state, for use via state lists.
 */
public class ParallaxScrimageView extends FourThreeImageView {
	
	public static final Property<ParallaxScrimageView, Integer> OFFSET = Property.of(ParallaxScrimageView.class, Integer.class, "offset");
	private static final int[] STATE_PINNED = {R.attr.state_pinned};
	private final Paint scrimPaint;
	private int imageOffset;
	private int minOffset;
	private Rect clipBounds = new Rect();
	private float scrimAlpha = 0f;
	private float maxScrimAlpha = 1f;
	private int scrimColor = Color.TRANSPARENT;
	private float parallaxFactor = -0.5f;
	private boolean isPinned = false;
	private boolean immediatePin = false;
	
	public ParallaxScrimageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.ParallaxScrimageView);
		if (a.hasValue(R.styleable.ParallaxScrimageView_scrimColor)) {
			scrimColor = a.getColor(R.styleable.ParallaxScrimageView_scrimColor, scrimColor);
		}
		if (a.hasValue(R.styleable.ParallaxScrimageView_scrimAlpha)) {
			scrimAlpha = a.getFloat(R.styleable.ParallaxScrimageView_scrimAlpha, scrimAlpha);
		}
		if (a.hasValue(R.styleable.ParallaxScrimageView_maxScrimAlpha)) {
			maxScrimAlpha = a.getFloat(R.styleable.ParallaxScrimageView_maxScrimAlpha,
					maxScrimAlpha);
		}
		if (a.hasValue(R.styleable.ParallaxScrimageView_parallaxFactor)) {
			parallaxFactor = a.getFloat(R.styleable.ParallaxScrimageView_parallaxFactor,
					parallaxFactor);
		}
		a.recycle();
		
		scrimPaint = new Paint();
		scrimPaint.setColor(ColorUtils.modifyAlpha(scrimColor, scrimAlpha));
	}
	
	public int getOffset() {
		return (int) getTranslationY();
	}
	
	public void setOffset(int offset) {
		offset = Math.max(minOffset, offset);
		if (offset != getTranslationY()) {
			setTranslationY(offset);
			imageOffset = (int) (offset * parallaxFactor);
			clipBounds.set(0, -offset, getWidth(), getHeight());
			setClipBounds(clipBounds);
			setScrimAlpha(Math.min(
					((float) -offset / getMinimumHeight()) * maxScrimAlpha, maxScrimAlpha));
			postInvalidateOnAnimation();
		}
		setPinned(offset == minOffset);
	}
	
	public void setScrimColor(@ColorInt int scrimColor) {
		if (this.scrimColor != scrimColor) {
			this.scrimColor = scrimColor;
			postInvalidateOnAnimation();
		}
	}
	
	public void setScrimAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
		if (scrimAlpha != alpha) {
			scrimAlpha = alpha;
			scrimPaint.setColor(ColorUtils.modifyAlpha(scrimColor, scrimAlpha));
			postInvalidateOnAnimation();
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (h > getMinimumHeight()) {
			minOffset = getMinimumHeight() - h;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (imageOffset != 0) {
			final int saveCount = canvas.save();
			canvas.translate(0f, imageOffset);
			super.onDraw(canvas);
			canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);
			canvas.restoreToCount(saveCount);
		} else {
			super.onDraw(canvas);
			canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);
		}
	}
	
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isPinned) {
			mergeDrawableStates(drawableState, STATE_PINNED);
		}
		return drawableState;
	}
	
	public boolean isPinned() {
		return isPinned;
	}
	
	public void setPinned(boolean isPinned) {
		if (this.isPinned != isPinned) {
			this.isPinned = isPinned;
			refreshDrawableState();
			if (isPinned && immediatePin) {
				jumpDrawablesToCurrentState();
			}
		}
	}
	
	public boolean isImmediatePin() {
		return immediatePin;
	}
	
	/**
	 * As the pinned state is designed to work with a {@see StateListAnimator}, we may want to short
	 * circuit this animation in certain situations e.g. when flinging a list.
	 */
	public void setImmediatePin(boolean immediatePin) {
		this.immediatePin = immediatePin;
	}
}


