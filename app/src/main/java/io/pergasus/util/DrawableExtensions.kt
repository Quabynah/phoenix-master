/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

@file:JvmName("DrawableUtils")

package io.pergasus.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat

fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

fun drawableToBitmap(context: Context, @DrawableRes drawableId: Int) =
        ContextCompat.getDrawable(context, drawableId)?.toBitmap()

fun Drawable.isAnimated() = this is Animatable

val LayerDrawable.layers: List<Drawable>
    get() = (0 until numberOfLayers).map { getDrawable(it) }

