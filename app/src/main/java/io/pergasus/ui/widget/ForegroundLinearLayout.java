/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:30 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import io.pergasus.R;

/**
 * See https://gist.github.com/chrisbanes/9091754
 */
public class ForegroundLinearLayout extends LinearLayout {
	
	private Drawable mForeground;
	
	public ForegroundLinearLayout(Context context) {
		super(context);
	}
	
	public ForegroundLinearLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public ForegroundLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundView,
				defStyle, 0);
		final Drawable d = a.getDrawable(R.styleable.ForegroundView_android_foreground);
		if (d != null) {
			setForeground(d);
		}
		a.recycle();
	}
	
	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || (who == mForeground);
	}
	
	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (mForeground != null) mForeground.jumpToCurrentState();
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mForeground != null && mForeground.isStateful()) {
			mForeground.setState(getDrawableState());
		}
	}
	
	/**
	 * Returns the drawable used as the foreground of this layout. The
	 * foreground drawable, if non-null, is always drawn on top of the children.
	 *
	 * @return A Drawable or null if no foreground was set.
	 */
	public Drawable getForeground() {
		return mForeground;
	}
	
	/**
	 * Supply a Drawable that is to be rendered on top of all of the child
	 * views in this layout.  Any padding in the Drawable will be taken
	 * into account by ensuring that the children are inset to be placed
	 * inside of the padding area.
	 *
	 * @param drawable The Drawable to be drawn on top of the children.
	 */
	public void setForeground(Drawable drawable) {
		if (mForeground != drawable) {
			if (mForeground != null) {
				mForeground.setCallback(null);
				unscheduleDrawable(mForeground);
			}
			
			mForeground = drawable;
			
			if (drawable != null) {
				setWillNotDraw(false);
				drawable.setCallback(this);
				if (drawable.isStateful()) {
					drawable.setState(getDrawableState());
				}
			} else {
				setWillNotDraw(true);
			}
			requestLayout();
			invalidate();
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mForeground != null) {
			mForeground.setBounds(0, 0, w, h);
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (mForeground != null) {
			mForeground.draw(canvas);
		}
	}
	
	@Override
	public void drawableHotspotChanged(float x, float y) {
		super.drawableHotspotChanged(x, y);
		if (mForeground != null) {
			mForeground.setHotspot(x, y);
		}
	}
}


