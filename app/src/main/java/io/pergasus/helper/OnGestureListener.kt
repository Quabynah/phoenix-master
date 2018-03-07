/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.helper

internal interface OnGestureListener {

    fun onDrag(dx: Float, dy: Float)

    fun onFling(startX: Float, startY: Float, velocityX: Float,
                velocityY: Float)

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)

}
