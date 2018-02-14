/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package in.uncod.android.bypass.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * A quote span with a nicer presentation
 */
public class FancyQuoteSpan implements LeadingMarginSpan {

	private final int lineColor;
	private final int lineWidth;
	private final int gapWidth;

	public FancyQuoteSpan(int quoteLineWidth,
	                      int quoteLineIndent,
	                      @ColorInt int quoteLineColor) {
		super();
		lineWidth = quoteLineWidth;
		gapWidth = quoteLineIndent;
		lineColor = quoteLineColor;
	}

	public int getLeadingMargin(boolean first) {
		return lineWidth + gapWidth;
	}

	public void drawLeadingMargin(Canvas c,
	                              Paint p,
	                              int x,
	                              int dir,
	                              int top,
	                              int baseline,
	                              int bottom,
	                              CharSequence text,
	                              int start,
	                              int end,
	                              boolean first,
	                              Layout layout) {
		Paint.Style prevStyle = p.getStyle();
		int prevColor = p.getColor();
		p.setStyle(Paint.Style.FILL);
		p.setColor(lineColor);
		c.drawRect(x, top, x + dir * lineWidth, bottom, p);
		p.setStyle(prevStyle);
		p.setColor(prevColor);
	}
}
