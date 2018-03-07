/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.helper

import android.view.MotionEvent

/**
 * A callback to be invoked when the ImageView is flung with a single
 * touch
 */
interface OnSingleFlingListener {

    /**
     * A callback to receive where the user flings on a ImageView. You will receive a callback if
     * the user flings anywhere on the view.
     *
     * @param e1        MotionEvent the user first touch.
     * @param e2        MotionEvent the user last touch.
     * @param velocityX distance of user's horizontal fling.
     * @param velocityY distance of user's vertical fling.
     */
    fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean
}
