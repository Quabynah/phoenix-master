/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

@file:JvmName("MathUtils")

package io.pergasus.util

fun constrain(min: Float, max: Float, v: Float) = v.coerceIn(min, max)
