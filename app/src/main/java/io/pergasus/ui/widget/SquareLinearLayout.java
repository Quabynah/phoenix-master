/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:36 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.util.AttributeSet;

/**
 * An extension to ForegroundLinearLayout that is always square.
 */
public class SquareLinearLayout extends ForegroundLinearLayout {
	
	public SquareLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// pass width spec for *both* width & height to get a square tile
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
	
	@Override
	public boolean hasOverlappingRendering() {
		return false;
	}
}

