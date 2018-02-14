/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.span;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:44 AM.
 * Package name : io.shoppingmart.ui.span
 */

import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

import io.pergasus.util.ColorUtils;

/**
 * An extension to {@link ForegroundColorSpan} which allows updating the color or alpha component.
 * Note that Spans cannot invalidate themselves so consumers must ensure that the Spannable is
 * refreshed themselves.
 */
public class TextColorSpan extends ForegroundColorSpan {
	
	private @ColorInt
	int color;
	
	public TextColorSpan(@ColorInt int color) {
		super(color);
		this.color = color;
	}
	
	public @ColorInt
	int getColor() {
		return color;
	}
	
	public void setColor(@ColorInt int color) {
		this.color = color;
	}
	
	public void setAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
		color = ColorUtils.modifyAlpha(color, alpha);
	}
	
	@Override
	public void updateDrawState(TextPaint ds) {
		ds.setColor(color);
	}
}


