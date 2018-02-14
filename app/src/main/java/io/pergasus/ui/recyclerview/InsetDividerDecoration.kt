/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView

/**
 * A decoration which draws a horizontal divider between [RecyclerView.ViewHolder]s of a given
 * type; with a left inset.
 */
class InsetDividerDecoration(private val dividedClass: Class<*>,
                             private val height: Int,
                             private val inset: Int,
                             @ColorInt dividerColor: Int) : RecyclerView.ItemDecoration() {
    private val paint: Paint = Paint()

    init {
        paint.color = dividerColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = height.toFloat()
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        val childCount = parent.childCount
        if (childCount < 2) return

        val lm = parent.layoutManager
        val lines = FloatArray(childCount * 4)
        var hasDividers = false

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)

            if (viewHolder.javaClass == dividedClass) {
                // skip if this *or next* view is activated
                if (child.isActivated || i + 1 < childCount && parent.getChildAt(i + 1).isActivated) {
                    continue
                }
                lines[i * 4] = (inset + lm.getDecoratedLeft(child)).toFloat()
                lines[i * 4 + 2] = lm.getDecoratedRight(child).toFloat()
                val y = lm.getDecoratedBottom(child) + child.translationY.toInt() - height
                lines[i * 4 + 1] = y.toFloat()
                lines[i * 4 + 3] = y.toFloat()
                hasDividers = true
            }
        }
        if (hasDividers) {
            canvas.drawLines(lines, paint)
        }
    }
}
