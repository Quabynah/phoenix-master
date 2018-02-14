/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.glide

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import io.pergasus.R
import io.pergasus.ui.widget.BadgedFourThreeImageView
import io.pergasus.util.ColorUtils
import io.pergasus.util.ViewUtils
import io.pergasus.util.isAnimated

class PhoenixTarget(private val badgedImageView: BadgedFourThreeImageView,
                    private val autoplayGifs: Boolean) : DrawableImageViewTarget(badgedImageView)
        ,Palette.PaletteAsyncListener{

    override fun onResourceReady(drawable: Drawable, transition: Transition<in Drawable>?) {
        super.onResourceReady(drawable, transition)
        val isAnimated = drawable.isAnimated()
        if (!autoplayGifs && isAnimated){
            (drawable as GifDrawable).stop()
        }
    }

    override fun onGenerated(palette: Palette) {
        badgedImageView.foreground = ViewUtils.createRipple(palette, 0.25f,0.5f,ContextCompat
                .getColor(view.context, R.color.mid_grey),true)
    }

    private fun setBadgeColor(){
        val biv = badgedImageView

        biv.drawable.getBitmap()?.let {
            val badgePos = biv.badgeBounds
            val scale = it.width.toFloat() / biv.width
            val left = (badgePos.left * scale).toInt()
            val top = (badgePos.top * scale).toInt()
            val width = (badgePos.width() * scale).toInt()
            val height = (badgePos.height() * scale).toInt()
            val corner = Bitmap.createBitmap(it,left,top,width,height)
            val isDark = ColorUtils.isDark(corner)
            corner.recycle()
            biv.setBadgeColor(ContextCompat.getColor(biv.context,if (isDark) R.color
                    .gif_badge_dark_image else R.color.gif_badge_light_image))
        }

    }

    
}