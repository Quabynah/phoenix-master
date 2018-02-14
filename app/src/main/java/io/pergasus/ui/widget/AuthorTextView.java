/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:39 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.util.AttributeSet;

import io.pergasus.R;

/**
 * An extension to TextView which supports a custom state of {@link #STATE_ORIGINAL_POSTER} for
 * denoting that a comment author was the original poster.
 */
public class AuthorTextView extends BaselineGridTextView {
	
	private static final int[] STATE_ORIGINAL_POSTER = {R.attr.state_original_poster};
	
	private boolean isOP = false;
	
	public AuthorTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isOP) {
			mergeDrawableStates(drawableState, STATE_ORIGINAL_POSTER);
		}
		return drawableState;
	}
	
	public boolean isOriginalPoster() {
		return isOP;
	}
	
	public void setOriginalPoster(boolean isOP) {
		if (this.isOP != isOP) {
			this.isOP = isOP;
			refreshDrawableState();
		}
	}
}

