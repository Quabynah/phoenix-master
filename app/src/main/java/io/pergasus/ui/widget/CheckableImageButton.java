/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:41 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.widget.Checkable;
import android.widget.ImageButton;

/**
 * An extension to {@link ImageButton} which implements the {@link Checkable} interface.
 */
@SuppressLint("AppCompatCustomView")
public class CheckableImageButton extends ImageButton implements Checkable {
	
	private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
	
	private boolean isChecked;
	
	public CheckableImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
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
	
	@Override // borrowed from CompoundButton#performClick()
	public boolean performClick() {
		toggle();
		boolean handled = super.performClick();
		if (!handled) {
			// View only makes a sound effect if the onClickListener was
			// called, so we'll need to make one here instead.
			playSoundEffect(SoundEffectConstants.CLICK);
		}
		return handled;
	}
	
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}
}


