/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:37 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import io.pergasus.R;


/**
 * A view which supports a minimum vertical offset (i.e. translation Y) and has a custom pinned
 * state.
 */
public class PinnedOffsetView extends View {
	
	private static final int[] STATE_PINNED = {R.attr.state_pinned};
	
	private int minOffset;
	private boolean isPinned = false;
	
	public PinnedOffsetView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public PinnedOffsetView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public PinnedOffsetView(Context context, AttributeSet attrs, int defStyleAttr, int
			defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	public void setOffset(int offset) {
		offset = Math.max(minOffset, offset);
		if (getTranslationY() != offset) {
			setTranslationY(offset);
			setPinned(offset == minOffset);
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		minOffset = -(h - getMinimumHeight());
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
		}
	}
}


