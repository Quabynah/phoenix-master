/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;


import android.content.Context;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Method;

/**
 * Utility methods for working with the keyboard
 */
public class ImeUtils {
	
	private ImeUtils() {
	}
	
	public static void showIme(@NonNull View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService
				(Context.INPUT_METHOD_SERVICE);
		// the public methods don't seem to work for me, so… reflection.
		try {
			Method showSoftInputUnchecked = InputMethodManager.class.getMethod(
					"showSoftInputUnchecked", int.class, ResultReceiver.class);
			showSoftInputUnchecked.setAccessible(true);
			showSoftInputUnchecked.invoke(imm, 0, null);
		} catch (Exception e) {
			// ho hum
		}
	}
	
	public static void hideIme(@NonNull View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context
				.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
}

