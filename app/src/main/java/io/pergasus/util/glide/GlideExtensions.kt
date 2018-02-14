/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

@file:JvmName("GlideUtils")

package io.pergasus.util.glide

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import com.bumptech.glide.load.resource.gif.GifDrawable
import io.pergasus.util.layers

fun Drawable.getBitmap(): Bitmap? {
    if (this is TransitionDrawable) {
        layers.forEach {
            val bmp = it.getBitmap()
            if (bmp != null) return bmp
        }
    }
    if (this is BitmapDrawable) {
        return bitmap
    } else if (this is GifDrawable) {
        return firstFrame
    }
    return null
}
