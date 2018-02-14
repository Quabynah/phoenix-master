/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.START
import io.pergasus.R
import io.pergasus.ui.CartActivity
import io.pergasus.util.setTranslation

class CartTouchHelperCallback(private val listener: CartSwipeDismissListener,
                              context: Context) : ItemTouchHelper.SimpleCallback(0, START) {

    private val backgroundColor: Int
    private val shadowColor: Int
    private val deleteColor: Int
    private val iconPadding: Int
    private val topShadowHeight: Float
    private val bottomShadowHeight: Float
    private val sideShadowWidth: Float

    // lazily initialized later
    private var initialized = false
    private var iconColorFilter: Int
    private var deleteIcon: Drawable? = null
    private var circlePaint: Paint? = null
    private var leftShadowPaint: Paint? = null
    private var topShadowPaint: Paint? = null
    private var bottomShadowPaint: Paint? = null

    init {
        val res = context.resources
        backgroundColor = ContextCompat.getColor(context, R.color.background_super_dark)
        shadowColor = ContextCompat.getColor(context, R.color.shadow)
        deleteColor = ContextCompat.getColor(context, R.color.delete)
        iconColorFilter = deleteColor
        iconPadding = res.getDimensionPixelSize(R.dimen.padding_normal)
        // faking elevation light-source; so use different shadow sizes
        topShadowHeight = res.getDimension(R.dimen.spacing_micro)
        bottomShadowHeight = topShadowHeight / 2f
        sideShadowWidth = topShadowHeight * 3f / 4f
    }

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?,
                        target: RecyclerView.ViewHolder?): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val swipeDir = if ((viewHolder as CartActivity.CartViewHolder).isSwipeable) START else 0
        return ItemTouchHelper.Callback.makeMovementFlags(0, swipeDir)
    }

    // make deleting a deliberate gesture
    override fun getSwipeEscapeVelocity(defaultValue: Float) = defaultValue * 5f

    override fun isLongPressDragEnabled() = false

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        // bail fast if there isn't a swipe
        if (dX == 0f) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val left = viewHolder.itemView.left.toFloat()
        val top = viewHolder.itemView.top.toFloat()
        val right = viewHolder.itemView.right.toFloat()
        val bottom = viewHolder.itemView.bottom.toFloat()
        val width = right - left
        val height = bottom - top
        val saveCount = c.save()

        // clip to the 'revealed' area
        c.clipRect(right + dX, top, right, bottom)
        c.drawColor(backgroundColor)

        // lazy initialize some vars
        initialize(recyclerView.context)

        // variables dependent upon gesture progress
        val progress = -dX / width
        val swipeThreshold = getSwipeThreshold(viewHolder)
        val thirdThreshold = swipeThreshold / 3f
        val iconPopThreshold = swipeThreshold + 0.125f
        val iconPopFinishedThreshold = iconPopThreshold + 0.125f
        var opacity = 1f
        var iconScale = 1f
        var circleRadius = 0f
        var iconColor = deleteColor
        when (progress) {
            in 0f..thirdThreshold -> {
                // fade in
                opacity = progress / thirdThreshold
            }
            in thirdThreshold..swipeThreshold -> {
                // scale icon down to 0.9
                iconScale = 1f -
                        (((progress - thirdThreshold) / (swipeThreshold - thirdThreshold)) * 0.1f)
            }
            else -> {
                // draw circle and switch icon color
                circleRadius = (progress - swipeThreshold) * width * CIRCLE_ACCELERATION
                iconColor = Color.WHITE
                // scale icon up to 1.2 then back down to 1
                iconScale = when (progress) {
                    in swipeThreshold..iconPopThreshold -> {
                        0.9f + ((progress - swipeThreshold) / (iconPopThreshold - swipeThreshold)) *
                                0.3f
                    }
                    in iconPopThreshold..iconPopFinishedThreshold -> {
                        1.2f - (((progress - iconPopThreshold) /
                                (iconPopFinishedThreshold - iconPopThreshold)) * 0.2f)
                    }
                    else -> 1f
                }
            }
        }

        deleteIcon?.let {
            val cx = right - iconPadding - it.intrinsicWidth / 2f
            val cy = top + height / 2f
            val halfIconSize = it.intrinsicWidth * iconScale / 2f
            it.setBounds((cx - halfIconSize).toInt(), (cy - halfIconSize).toInt(),
                    (cx + halfIconSize).toInt(), (cy + halfIconSize).toInt())
            it.alpha = (opacity * 255f).toInt()
            if (iconColor != iconColorFilter) {
                it.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                iconColorFilter = iconColor
            }
            if (circleRadius > 0f) {
                c.drawCircle(cx, cy, circleRadius, circlePaint)
            }
            it.draw(c)
        }

        // draw shadows to fake elevation of surrounding views
        topShadowPaint?.let {
            it.shader?.setTranslation(y = top)
            c.drawRect(left, top, right, top + topShadowHeight, it)
        }
        bottomShadowPaint?.let {
            it.shader?.setTranslation(y = bottom - bottomShadowHeight)
            c.drawRect(left, bottom - bottomShadowHeight, right, bottom, it)
        }
        leftShadowPaint?.let {
            val shadowLeft = right + dX
            it.shader?.setTranslation(x = shadowLeft)
            c.drawRect(shadowLeft, top, shadowLeft + sideShadowWidth, bottom, it)
        }

        c.restoreToCount(saveCount)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun initialize(context: Context) {
        if (!initialized) {
            deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
            topShadowPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, topShadowHeight, shadowColor, 0, Shader.TileMode.CLAMP)
            }
            bottomShadowPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, bottomShadowHeight, 0, shadowColor, Shader.TileMode.CLAMP)
            }
            leftShadowPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, sideShadowWidth, 0f, shadowColor, 0, Shader.TileMode.CLAMP)
            }
            circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = deleteColor
            }
            initialized = true
        }
    }

    companion object {
        // expand the circle rapidly once it shows, don't track swipe 1:1
        private const val CIRCLE_ACCELERATION = 3f
    }
}