/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.ColorInt;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.StaticLayout.Builder;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import in.uncod.android.bypass.style.TouchableUrlSpan;
import io.pergasus.R;

/**
 * A view for displaying text that is will be overlapped by a Floating Action Button (FAB).
 * This view will indent itself at the given overlap point (as specified by
 * {@link #setFabOverlapGravity(int)}) to flow around it.
 * <p>
 * Not actually a TextView but conforms to many of it's idioms.
 */

@TargetApi(VERSION_CODES.M)
public class FabOverlapTextView extends View {
	
	private static final int DEFAULT_TEXT_SIZE_SP = 14;
	
	private int fabOverlapHeight;
	private int fabOverlapWidth;
	private int fabGravity;
	private int lineHeightHint;
	private int unalignedTopPadding;
	private int unalignedBottomPadding;
	private int breakStrategy;
	private StaticLayout layout;
	private CharSequence text;
	private TextPaint paint;
	private TouchableUrlSpan pressedSpan;
	
	public FabOverlapTextView(Context context) {
		this(context, null);
	}
	
	public FabOverlapTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}
	
	public FabOverlapTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}
	
	public FabOverlapTextView(Context context, AttributeSet attrs, int defStyleAttr, int
			defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FabOverlapTextView);
		
		setFabOverlapGravity(a.getInt(R.styleable.FabOverlapTextView_fabGravity,
				Gravity.BOTTOM | Gravity.RIGHT));
		setFabOverlapHeight(a.getDimensionPixelSize(
				R.styleable.FabOverlapTextView_fabOverlayHeight, 0));
		setFabOverlapWidth(a.getDimensionPixelSize(
				R.styleable.FabOverlapTextView_fabOverlayWidth, 0));
		
		// first check the TextAppearance for text attributes
		if (a.hasValue(R.styleable.FabOverlapTextView_android_textAppearance)) {
			final int textAppearanceId = a.getResourceId(
					R.styleable.FabOverlapTextView_android_textAppearance,
					android.R.style.TextAppearance);
			TypedArray ta = getContext().obtainStyledAttributes(textAppearanceId,
					R.styleable.FabOverlapTextView);
			parseTextAppearance(ta);
			ta.recycle();
		}
		
		// then check view for text attributes
		parseTextAppearance(a);
		breakStrategy = a.getInt(R.styleable.FabOverlapTextView_android_breakStrategy,
				Layout.BREAK_STRATEGY_BALANCED);
		
		unalignedTopPadding = getPaddingTop();
		unalignedBottomPadding = getPaddingBottom();
		a.recycle();
	}
	
	public void setFabOverlapGravity(int fabGravity) {
		// we only really support [top|bottom][left|right|start|end]
		this.fabGravity = GravityCompat.getAbsoluteGravity(fabGravity, getLayoutDirection());
	}
	
	public void setFabOverlapHeight(int fabOverlapHeight) {
		this.fabOverlapHeight = fabOverlapHeight;
	}
	
	public void setFabOverlapWidth(int fabOverlapWidth) {
		this.fabOverlapWidth = fabOverlapWidth;
	}
	
	public void setText(CharSequence text) {
		this.text = text;
		layout = null;
		recompute(getWidth());
		requestLayout();
	}
	
	public void setTextSize(int textSize) {
		paint.setTextSize(textSize);
	}
	
	public void setTextColor(@ColorInt int color) {
		paint.setColor(color);
	}
	
	public void setTypeface(Typeface typeface) {
		paint.setTypeface(typeface);
	}
	
	public void setLetterSpacing(float letterSpacing) {
		paint.setLetterSpacing(letterSpacing);
	}
	
	public void setFontFeatureSettings(String fontFeatureSettings) {
		paint.setFontFeatureSettings(fontFeatureSettings);
	}
	
	private void parseTextAppearance(TypedArray a) {
		if (a.hasValue(R.styleable.FabOverlapTextView_android_fontFamily)) {
			try {
				Typeface font = ResourcesCompat.getFont(getContext(),
						a.getResourceId(R.styleable.FabOverlapTextView_android_fontFamily, 0));
				if (font != null) {
					setTypeface(font);
				}
			} catch (Resources.NotFoundException nfe) {
				// swallow - use default typeface
			}
		}
		
		if (a.hasValue(R.styleable.FabOverlapTextView_android_textColor)) {
			setTextColor(a.getColor(R.styleable.FabOverlapTextView_android_textColor, 0));
		}
		if (a.hasValue(R.styleable.FabOverlapTextView_android_textSize)) {
			int defaultTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
					DEFAULT_TEXT_SIZE_SP, getResources().getDisplayMetrics());
			setTextSize(a.getDimensionPixelSize(R.styleable.FabOverlapTextView_android_textSize,
					defaultTextSize));
		}
		if (a.hasValue(R.styleable.FabOverlapTextView_android_letterSpacing)) {
			setLetterSpacing(a.getFloat(R.styleable.FabOverlapTextView_android_letterSpacing, 0f));
		}
		if (a.hasValue(R.styleable.FabOverlapTextView_android_fontFeatureSettings)) {
			setFontFeatureSettings(
					a.getString(R.styleable.FabOverlapTextView_android_fontFeatureSettings));
		}
		if (a.hasValue(R.styleable.FabOverlapTextView_lineHeightHint)) {
			lineHeightHint =
					a.getDimensionPixelSize(R.styleable.FabOverlapTextView_lineHeightHint, 0);
		}
	}
	
	private void recompute(int width) {
		if (text != null) {
			// work out the top padding and line height to align text to a 4dp grid
			final float fourDip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
					getResources().getDisplayMetrics());
			
			// ensure that the first line's baselines sits on 4dp grid by setting the top padding
			final FontMetricsInt fm = paint.getFontMetricsInt();
			final int gridAlignedTopPadding = (int) (fourDip * (float)
					Math.ceil((unalignedTopPadding + Math.abs(fm.ascent)) / fourDip)
					- Math.ceil(Math.abs(fm.ascent)));
			super.setPadding(
					getPaddingLeft(), gridAlignedTopPadding, getPaddingTop(), getPaddingBottom());
			
			// ensures line height is a multiple of 4dp
			final int fontHeight = Math.abs(fm.ascent - fm.descent) + fm.leading;
			final int baselineAlignedLineHeight =
					(int) (fourDip * (float) Math.ceil(lineHeightHint / fourDip));
			
			// before we can workout indents we need to know how many lines of text there are;
			// so we need to create a temporary layout :(
			if (VERSION.SDK_INT >= VERSION_CODES.M) {
				layout = Builder.obtain(text, 0, text.length(), paint, width)
						.setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f)
						.setBreakStrategy(breakStrategy)
						.build();
			}
			final int preIndentedLineCount = layout.getLineCount();
			
			// now we can calculate the indents required for the given fab gravity
			final boolean gravityTop = (fabGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP;
			final boolean gravityLeft =
					(fabGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT;
			// we want to iterate forward/backward over the lines depending on whether the fab
			// overlap vertical gravity is top/bottom
			int currentLine = gravityTop ? 0 : preIndentedLineCount - 1;
			int remainingHeightOverlap = fabOverlapHeight -
					(gravityTop ? getPaddingTop() : getPaddingBottom());
			final int[] leftIndents = new int[preIndentedLineCount];
			final int[] rightIndents = new int[preIndentedLineCount];
			do {
				if (remainingHeightOverlap > 0) {
					// still have overlap height to consume, set the appropriate indent
					leftIndents[currentLine] = gravityLeft ? fabOverlapWidth : 0;
					rightIndents[currentLine] = gravityLeft ? 0 : fabOverlapWidth;
					remainingHeightOverlap -= baselineAlignedLineHeight;
				} else {
					// have consumed the overlap height: no indent
					leftIndents[currentLine] = 0;
					rightIndents[currentLine] = 0;
				}
				if (gravityTop) { // iterate forward over the lines
					currentLine++;
				} else { // iterate backward over the lines
					currentLine--;
				}
			} while (gravityTop ? currentLine < preIndentedLineCount : currentLine >= 0);
			
			// now that we know the indents, create the actual layout
			if (VERSION.SDK_INT >= VERSION_CODES.M) {
				layout = Builder.obtain(text, 0, text.length(), paint, width)
						.setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f)
						.setIndents(leftIndents, rightIndents)
						.setBreakStrategy(breakStrategy)
						.build();
			}
			
			// ensure that the view's height sits on the grid (as we've changed padding etc).
			final int height = getPaddingTop() + layout.getHeight() + getPaddingBottom();
			final float overhang = height % fourDip;
			if (overhang != 0) {
				super.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
						unalignedBottomPadding + (int) (fourDip - overhang));
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			throw new IllegalArgumentException("FabOverlapTextView requires a constrained width");
		}
		int layoutWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() -
				getPaddingRight();
		if (layout == null || layoutWidth != layout.getWidth()) {
			recompute(layoutWidth);
		}
		setMeasuredDimension(
				getPaddingLeft() + (layout != null ? layout.getWidth() : 0) + getPaddingRight(),
				getPaddingTop() + (layout != null ? layout.getHeight() : 0) + getPaddingBottom());
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (layout != null) {
			canvas.translate(getPaddingLeft(), getPaddingTop());
			layout.draw(canvas);
		}
	}
	
	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		super.setPadding(left, top, right, bottom);
		unalignedTopPadding = top;
		unalignedBottomPadding = bottom;
		if (layout != null) recompute(layout.getWidth());
	}
	
	@Override
	public void setPaddingRelative(int start, int top, int end, int bottom) {
		super.setPaddingRelative(start, top, end, bottom);
		unalignedTopPadding = top;
		unalignedBottomPadding = bottom;
		if (layout != null) recompute(layout.getWidth());
	}
	
	/**
	 * This is why you don't implement your own TextView kids; you have to handle everything!
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!(text instanceof Spanned)) return super.onTouchEvent(event);
		
		Spannable spannedText = (Spannable) text;
		
		boolean handled = false;
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pressedSpan = getPressedSpan(spannedText, event);
			if (pressedSpan != null) {
				pressedSpan.setPressed(true);
				Selection.setSelection(spannedText, spannedText.getSpanStart(pressedSpan),
						spannedText.getSpanEnd(pressedSpan));
				handled = true;
				postInvalidateOnAnimation();
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			TouchableUrlSpan touchedSpan = getPressedSpan(spannedText, event);
			if (pressedSpan != null && touchedSpan != pressedSpan) {
				pressedSpan.setPressed(false);
				pressedSpan = null;
				Selection.removeSelection(spannedText);
				postInvalidateOnAnimation();
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (pressedSpan != null) {
				pressedSpan.setPressed(false);
				pressedSpan.onClick(this);
				handled = true;
				postInvalidateOnAnimation();
			}
			pressedSpan = null;
			Selection.removeSelection(spannedText);
		} else {
			if (pressedSpan != null) {
				pressedSpan.setPressed(false);
				handled = true;
				postInvalidateOnAnimation();
			}
			pressedSpan = null;
			Selection.removeSelection(spannedText);
		}
		return handled;
	}
	
	private TouchableUrlSpan getPressedSpan(Spannable spannable, MotionEvent event) {
		
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		x -= getPaddingLeft();
		y -= getPaddingTop();
		
		x += getScrollX();
		y += getScrollY();
		
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


