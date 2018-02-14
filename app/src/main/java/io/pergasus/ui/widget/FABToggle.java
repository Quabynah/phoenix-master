/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:38 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

/**
 * A {@link Checkable} {@link ImageButton} which can be offset vertically.
 */
@SuppressLint("AppCompatCustomView")
public class FABToggle extends ImageButton implements Checkable {
	
	private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
	
	private boolean isChecked = false;
	private int minOffset;
	
	public FABToggle(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setOffset(int offset) {
		if (offset != getTranslationY()) {
			offset = Math.max(minOffset, offset);
			setTranslationY(offset);
		}
	}
	
	public void setMinOffset(int minOffset) {
		this.minOffset = minOffset;
	}
	
	public boolean isChecked() {
		return isChecked;
	}
	
	public void setChecked(boolean isChecked) {
		if (this.isChecked != isChecked) {
			this.isChecked = isChecked;
			refreshDrawableState();
		}
	}
	
	public void toggle() {
		setChecked(!isChecked);
	}
	
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}
}


