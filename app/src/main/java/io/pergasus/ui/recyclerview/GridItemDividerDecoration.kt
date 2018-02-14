/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DimenRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView

/**
 * A [RecyclerView.ItemDecoration] which draws dividers (along the right & bottom)
 * for certain [RecyclerView.ViewHolder] types.
 */
class GridItemDividerDecoration(private val dividerSize: Int,
                                @ColorInt dividerColor: Int) : RecyclerView.ItemDecoration() {
    private val paint: Paint = Paint()

    init {
        paint.color = dividerColor
        paint.style = Paint.Style.FILL
    }

    constructor(context: Context,
                @DimenRes dividerSizeResId: Int,
                @ColorRes dividerColorResId: Int) : this(context.resources.getDimensionPixelSize(dividerSizeResId),
            ContextCompat.getColor(context, dividerColorResId))

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        if (parent.isAnimating) return

        val childCount = parent.childCount
        val lm = parent.layoutManager
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)

            if (viewHolder is Divided) {
                val right = lm.getDecoratedRight(child)
                val bottom = lm.getDecoratedBottom(child)
                // draw the bottom divider
                canvas.drawRect(lm.getDecoratedLeft(child).toFloat(),
                        (bottom - dividerSize).toFloat(),
                        right.toFloat(),
                        bottom.toFloat(),
                        paint)
                // draw the right edge divider
                canvas.drawRect((right - dividerSize).toFloat(),
                        lm.getDecoratedTop(child).toFloat(),
                        right.toFloat(),
                        (bottom - dividerSize).toFloat(),
                        paint)
            }
        }
    }
}
