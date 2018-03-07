/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.helper

import android.annotation.TargetApi
import android.os.Build
import android.view.View

internal object Compat {

    private val SIXTY_FPS_INTERVAL = 1000 / 60

    fun postOnAnimation(view: View, runnable: Runnable) {
        postOnAnimationJellyBean(view, runnable)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun postOnAnimationJellyBean(view: View, runnable: Runnable) {
        view.postOnAnimation(runnable)
    }
}
