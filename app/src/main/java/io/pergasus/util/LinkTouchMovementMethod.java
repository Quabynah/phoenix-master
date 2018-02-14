/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.Objects;

import in.uncod.android.bypass.style.TouchableUrlSpan;

/**
 * A movement method that only highlights any touched
 * {@link TouchableUrlSpan}s
 *
 * Adapted from  http://stackoverflow.com/a/20905824
 */
public class LinkTouchMovementMethod extends LinkMovementMethod {
	
	
	private static LinkTouchMovementMethod instance;
	private TouchableUrlSpan pressedSpan;
	
	public static MovementMethod getInstance() {
		if (instance == null)
			instance = new LinkTouchMovementMethod();
		
		return instance;
	}
	
	@Override
	public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent event) {
		boolean handled = false;
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pressedSpan = getPressedSpan(textView, spannable, event);
			if (pressedSpan != null) {
				pressedSpan.setPressed(true);
				Selection.setSelection(spannable, spannable.getSpanStart(pressedSpan),
						spannable.getSpanEnd(pressedSpan));
				handled = true;
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			TouchableUrlSpan touchedSpan = getPressedSpan(textView, spannable, event);
			if (pressedSpan != null && !Objects.equals(touchedSpan, pressedSpan)) {
				pressedSpan.setPressed(false);
				pressedSpan = null;
				Selection.removeSelection(spannable);
			}
		} else {
			if (pressedSpan != null) {
				pressedSpan.setPressed(false);
				super.onTouchEvent(textView, spannable, event);
				handled = true;
			}
			pressedSpan = null;
			Selection.removeSelection(spannable);
		}
		return handled;
	}
	
	private TouchableUrlSpan getPressedSpan(TextView textView, Spannable spannable, MotionEvent
			event) {
		
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		x -= textView.getTotalPaddingLeft();
		y -= textView.getTotalPaddingTop();
		
		x += textView.getScrollX();
		y += textView.getScrollY();
		
		Layout layout = textView.getLayout();
		int line = layout.getLineForVertical(y);
		int off = layout.getOffsetForHorizontal(line, x);
		
		TouchableUrlSpan[] link = spannable.getSpans(off, off, TouchableUrlSpan.class);
		TouchableUrlSpan touchedSpan = null;
		if (link.length > 0) {
			touchedSpan = link[0];
		}
		return touchedSpan;
	}
	
}