/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package in.uncod.android.bypass.style;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

/**
 * A simple text span used to mark text that will be replaced by an image once it has been
 * downloaded. See {@link in.uncod.android.bypass.Bypass.LoadImageCallback}
 */
public class ImageLoadingSpan extends CharacterStyle {
	@Override
	public void updateDrawState(TextPaint textPaint) {
		// no-op
	}
}
