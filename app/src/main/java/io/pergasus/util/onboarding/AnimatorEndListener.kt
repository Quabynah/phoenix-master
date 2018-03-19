/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.onboarding

import android.animation.Animator

/**
 * Just sugar for code clean
 */
abstract class AnimatorEndListener : Animator.AnimatorListener {

    override fun onAnimationStart(animation: Animator) {
        //do nothing
    }

    override fun onAnimationCancel(animation: Animator) {
        //do nothing
    }

    override fun onAnimationRepeat(animation: Animator) {
        //do nothing
    }
}
