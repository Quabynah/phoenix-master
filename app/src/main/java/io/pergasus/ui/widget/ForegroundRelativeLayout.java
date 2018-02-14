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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

import io.pergasus.R;

/**
 * An extension to {@link RelativeLayout} which has a foreground drawable.
 */
public class ForegroundRelativeLayout extends RelativeLayout {
	
	private Drawable foreground;
	
	public ForegroundRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundView);
		
		final Drawable d = a.getDrawable(R.styleable.ForegroundView_android_foreground);
		if (d != null) {
			setForeground(d);
		}
		a.recycle();
		setOutlineProvider(ViewOutlineProvider.BOUNDS);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (foreground != null) {
			foreground.setBounds(0, 0, w, h);
		}
	}
	
	@Override
	public boolean hasOverlappingRendering() {
		return false;
	}
	
	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || (who == foreground);
	}
	
	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (foreground != null) foreground.jumpToCurrentState();
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (foreground != null && foreground.isStateful()) {
			foreground.setState(getDrawableState());
		}
	}
	
	/**
	 * Returns the drawable used as the foreground of this view. The
	 * foreground drawable, if non-null, is always drawn on top of the children.
	 *
	 * @return A Drawable or null if no foreground was set.
	 */
	public Drawable getForeground() {
		return foreground;
	}
	
	/**
	 * Supply a Drawable that is to be rendered on top of all of the child
	 * views within this layout.  Any padding in the Drawable will be taken
	 * into account by ensuring that the children are inset to be placed
	 * inside of the padding area.
	 *
	 * @param drawable The Drawable to be drawn on top of the children.
	 */
	public void setForeground(Drawable drawable) {
		if (foreground != drawable) {
			if (foreground != null) {
				foreground.setCallback(null);
				unscheduleDrawable(foreground);
			}
			
			foreground = drawable;
			
			if (foreground != null) {
				foreground.setBounds(getLeft(), getTop(), getRight(), getBottom());
				setWillNotDraw(false);
				foreground.setCallback(this);
				if (foreground.isStateful()) {
					foreground.setState(getDrawableState());
				}
			} else {
				setWillNotDraw(true);
			}
			invalidate();
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (foreground != null) {
			foreground.draw(canvas);
		}
	}
	
	@Override
	public void drawableHotspotChanged(float x, float y) {
		super.drawableHotspotChanged(x, y);
		if (foreground != null) {
			foreground.setHotspot(x, y);
		}
	}
}

