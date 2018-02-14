/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import io.pergasus.util.AnimUtils
import java.util.*

/**
 * A [RecyclerView.ItemAnimator] that fades & slides newly added items in from a given
 * direction.
 */
open class SlideInItemAnimator @JvmOverloads constructor(slideFromEdge: Int = Gravity.BOTTOM,
layoutDirection: Int = -1) : DefaultItemAnimator() {

    private val pendingAdds = ArrayList<RecyclerView.ViewHolder>(0)
    private val slideFromEdge: Int

    init {
        this.slideFromEdge = Gravity.getAbsoluteGravity(slideFromEdge, layoutDirection)
        addDuration = 160L
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.alpha = 0f
        when (slideFromEdge) {
            Gravity.LEFT -> holder.itemView.translationX = (-holder.itemView.width / 3).toFloat()
            Gravity.TOP -> holder.itemView.translationY = (-holder.itemView.height / 3).toFloat()
            Gravity.RIGHT -> holder.itemView.translationX = (holder.itemView.width / 3).toFloat()
            else // Gravity.BOTTOM
            -> holder.itemView.translationY = (holder.itemView.height / 3).toFloat()
        }
        pendingAdds.add(holder)
        return true
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        if (!pendingAdds.isEmpty()) {
            for (i in pendingAdds.indices.reversed()) {
                val holder = pendingAdds[i]
                holder.itemView.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .translationY(0f)
                        .setDuration(addDuration)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator) {
                                dispatchAddStarting(holder)
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                animation.listeners.remove(this)
                                dispatchAddFinished(holder)
                                dispatchFinishedWhenDone()
                            }

                            override fun onAnimationCancel(animation: Animator) {
                                clearAnimatedValues(holder.itemView)
                            }
                        }).interpolator = AnimUtils.getLinearOutSlowInInterpolator(
                        holder.itemView.context)
                pendingAdds.removeAt(i)
            }
        }
    }

    override fun endAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
        if (pendingAdds.remove(holder)) {
            dispatchAddFinished(holder)
            clearAnimatedValues(holder.itemView)
        }
        super.endAnimation(holder)
    }

    override fun endAnimations() {
        for (i in pendingAdds.indices.reversed()) {
            val holder = pendingAdds[i]
            clearAnimatedValues(holder.itemView)
            dispatchAddFinished(holder)
            pendingAdds.removeAt(i)
        }
        super.endAnimations()
    }

    override fun isRunning(): Boolean {
        return !pendingAdds.isEmpty() || super.isRunning()
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    private fun clearAnimatedValues(view: View) {
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
    }

}
/**
 * Default to sliding in upward.
 */// undefined layout dir; bottom isn't relative
