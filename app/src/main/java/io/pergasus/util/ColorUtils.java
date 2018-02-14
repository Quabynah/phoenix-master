/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;



import android.graphics.Bitmap;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility methods for working with colors.
 */
public class ColorUtils {
	
	public static final int IS_LIGHT = 0;
	public static final int IS_DARK = 1;
	public static final int LIGHTNESS_UNKNOWN = 2;
	private ColorUtils() {
	}
	
	/**
	 * Set the alpha component of {@code color} to be {@code alpha}.
	 */
	@ColorInt
	@CheckResult
	public static int modifyAlpha(@ColorInt int color,
	                              @IntRange(from = 0, to = 255) int alpha) {
		return (color & 0x00ffffff) | (alpha << 24);
	}
	
	/**
	 * Set the alpha component of {@code color} to be {@code alpha}.
	 */
	@ColorInt
	@CheckResult
	public static int modifyAlpha(@ColorInt int color,
	                              @FloatRange(from = 0f, to = 1f) float alpha) {
		return modifyAlpha(color, (int) (255f * alpha));
	}
	
	/**
	 * Checks if the most populous color in the given palette is dark
	 * Annoyingly we have to return this Lightness 'enum' rather than a boolean as palette isn't
	 * guaranteed to find the most populous color.
	 */
	@Lightness
	public static int isDark(Palette palette) {
		Palette.Swatch mostPopulous = getMostPopulousSwatch(palette);
		if (mostPopulous == null) return LIGHTNESS_UNKNOWN;
		return isDark(mostPopulous.getHsl()) ? IS_DARK : IS_LIGHT;
	}
	
	@Nullable
	public static Palette.Swatch getMostPopulousSwatch(Palette palette) {
		Palette.Swatch mostPopulous = null;
		if (palette != null) {
			for (Palette.Swatch swatch : palette.getSwatches()) {
				if (mostPopulous == null || swatch.getPopulation() > mostPopulous.getPopulation()) {
					mostPopulous = swatch;
				}
			}
		}
		return mostPopulous;
	}
	
	/**
	 * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
	 * with a large image!!
	 * Note: If palette fails then check the color of the central pixel
	 */
	public static boolean isDark(@NonNull Bitmap bitmap) {
		return isDark(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
	}
	
	/**
	 * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
	 * with a large image!! If palette fails then check the color of the specified pixel
	 */
	public static boolean isDark(@NonNull Bitmap bitmap, int backupPixelX, int backupPixelY) {
		// first try palette with a small color quant size
		Palette palette = Palette.from(bitmap).maximumColorCount(3).generate();
		return palette.getSwatches().isEmpty() ? isDark(bitmap.getPixel(backupPixelX, backupPixelY)) : isDark(palette) == IS_DARK;
	}
	
	/**
	 * Check that the lightness value (0–1)
	 */
	public static boolean isDark(float[] hsl) { // @Size(3)
		return hsl[2] < 0.5f;
	}
	
	/**
	 * Convert to HSL & check that the lightness value
	 */
	public static boolean isDark(@ColorInt int color) {
		float[] hsl = new float[3];
		android.support.v4.graphics.ColorUtils.colorToHSL(color, hsl);
		return isDark(hsl);
	}
	
	/**
	 * Calculate a variant of the color to make it more suitable for overlaying information. Light
	 * colors will be lightened and dark colors will be darkened
	 *
	 * @param color               the color to adjust
	 * @param isDark              whether {@code color} is light or dark
	 * @param lightnessMultiplier the amount to modify the color e.g. 0.1f will alter it by 10%
	 *
	 * @return the adjusted color
	 */
	@ColorInt
	public static int scrimify(@ColorInt int color,
	                           boolean isDark,
	                           @FloatRange(from = 0.0f, to = 1.0f) float lightnessMultiplier) {
		float[] hsl = new float[3];
		android.support.v4.graphics.ColorUtils.colorToHSL(color, hsl);
		
		if (isDark) {
			lightnessMultiplier = 1.0f - lightnessMultiplier;
		} else {
			lightnessMultiplier += 1.0f;
		}
		
		hsl[2] = MathUtils.constrain(0.0f, 1.0f, hsl[2] * lightnessMultiplier);
		return android.support.v4.graphics.ColorUtils.HSLToColor(hsl);
	}
	
	@ColorInt
	public static int scrimify(@ColorInt int color,
	                           @FloatRange(from = 0f, to = 1f) float lightnessMultiplier) {
		return scrimify(color, isDark(color), lightnessMultiplier);
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({IS_LIGHT, IS_DARK, LIGHTNESS_UNKNOWN})
	public @interface Lightness {
	}
	
}


