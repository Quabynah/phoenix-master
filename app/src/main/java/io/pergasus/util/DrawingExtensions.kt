/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

import android.graphics.Matrix
import android.graphics.Shader

fun Shader.setTranslation(x: Float = 0f, y: Float = 0f) {
    getLocalMatrix(matrix)
    matrix.setTranslate(x, y)
    setLocalMatrix(matrix)
}

private val matrix: Matrix by lazy(LazyThreadSafetyMode.NONE) {
    Matrix()
}
