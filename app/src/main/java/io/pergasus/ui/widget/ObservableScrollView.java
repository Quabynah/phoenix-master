/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget;

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Thu at 11:35 AM.
 * Package name : io.shoppingmart.ui.widget
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * An extension to {@link ScrollView} which exposes a scroll listener.
 */
public class ObservableScrollView extends ScrollView {
	
	private OnScrollListener listener;
	
	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setListener(OnScrollListener listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onScrollChanged(int currentScrollX,
	                               int currentScrollY,
	                               int oldScrollX,
	                               int oldScrollY) {
		super.onScrollChanged(currentScrollX, currentScrollY, oldScrollX, oldScrollY);
		if (listener != null) {
			listener.onScrolled(currentScrollY);
		}
	}
	
	public interface OnScrollListener {
		void onScrolled(int scrollY);
	}
}


