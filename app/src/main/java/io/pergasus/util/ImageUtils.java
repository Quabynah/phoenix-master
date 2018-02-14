/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;

/**
 * Utility methods for working with images.
 */
public class ImageUtils {

    private ImageUtils() { }

    public static Bitmap vectorToBitmap(Context context, Drawable vector) {
        final Bitmap bitmap = Bitmap.createBitmap(vector.getIntrinsicWidth(),
                vector.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        vector.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vector.draw(canvas);
        return bitmap;
    }

    public static Bitmap vectorToBitmap(Context context, @DrawableRes int vectorDrawableId) {
        return vectorToBitmap(context, context.getDrawable(vectorDrawableId));
    }
}
