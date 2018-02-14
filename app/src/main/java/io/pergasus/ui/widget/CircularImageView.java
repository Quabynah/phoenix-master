/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import io.pergasus.util.ViewUtils;

/**
 * An extension to image view that has a circular outline.
 */
public class CircularImageView extends ForegroundImageView {
	
	public CircularImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOutlineProvider(ViewUtils.CIRCULAR_OUTLINE);
		setClipToOutline(true);
	}
}

