/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

/**
 * Swipe listener for cart items
 */
interface CartSwipeDismissListener {
    fun onItemDismiss(position: Int)
}