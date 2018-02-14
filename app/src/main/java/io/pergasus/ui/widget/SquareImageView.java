/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Project : OnlineShoppingMart
 * Created by Dennis Bilson on Mon at 9:06 AM.
 * Package name : io.app.ui.widget
 */

//This is an extension of @Link{ForegroundImageView} that is always square
public class SquareImageView extends ForegroundImageView {
	
	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int squareHeight = MeasureSpec.makeMeasureSpec(100, MeasureSpec
				.AT_MOST);
		super.onMeasure(squareHeight, squareHeight);
	}
}
