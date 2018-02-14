/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:31 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.util.AttributeSet;

/**
 * A extension of ForegroundLinearLayout that is always 4:3 aspect ratio.
 */
public class FourThreeLinearLayout extends ForegroundLinearLayout {
	
	public FourThreeLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int fourThreeHeight = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) * 3 / 4,
				MeasureSpec.EXACTLY);
		super.onMeasure(widthMeasureSpec, fourThreeHeight);
	}
	
	@Override
	public boolean hasOverlappingRendering() {
		return false;
	}
}


