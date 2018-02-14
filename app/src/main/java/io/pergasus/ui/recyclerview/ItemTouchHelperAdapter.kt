/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

/**
 * Interface for events related to swipe dismissing filters
 */
interface ItemTouchHelperAdapter {
    fun onItemDismiss(position: Int)
}