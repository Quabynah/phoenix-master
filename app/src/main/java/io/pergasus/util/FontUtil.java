/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapted from github.com/romannurik/muzei/
 * <p/>
 * Also see https://code.google.com/p/android/issues/detail?id=9904
 */
public class FontUtil {

    private FontUtil() { }

    private static final Map<String, Typeface> sTypefaceCache = new HashMap<>();

    public static Typeface get(Context context, String font) {
        synchronized (sTypefaceCache) {
            if (!sTypefaceCache.containsKey(font)) {
                Typeface tf = Typeface.createFromAsset(
                        context.getApplicationContext().getAssets(), "fonts/" + font + ".ttf");
                sTypefaceCache.put(font, tf);
            }
            return sTypefaceCache.get(font);
        }
    }

    public static String getName(@NonNull Typeface typeface) {
        for (Map.Entry<String, Typeface> entry : sTypefaceCache.entrySet()) {
            if (entry.getValue() == typeface) {
                return entry.getKey();
            }
        }
        return null;
    }
}
