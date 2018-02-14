/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.widget

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.request.RequestOptions

import io.pergasus.R
import io.pergasus.util.ScrimUtil
import io.pergasus.util.glide.GlideApp

/**
 * Project : phoenix-master
 * Created by Dennis Bilson on Sat at 7:30 AM.
 * Package name : io.app.ui.widget
 */

/*This is a custom toast that inflates a layout and displays an image and a text*/
class ConfirmationToastView(private val host: Context, private val message: String?, private val imageUrl: Any?, private val label: String?) {

    fun create(): ConfirmationToastView {
        val confirmLogin = Toast(host.applicationContext)
        val v = LayoutInflater.from(host).inflate(R.layout
                .toast_logged_in_confirmation, null, false)
        if (message != null) {
            (v.findViewById(R.id.name) as TextView).text = message
        }else{
            (v.findViewById(R.id.name) as TextView).text = host.resources.getString(R.string.empty)
        }
        val imageView = v.findViewById<ImageView>(R.id.avatar)

        if (label != null) {
            (v.findViewById(R.id.toast_label) as TextView).text = label
        }

        // need to use app context here as the activity will be destroyed shortly
        if (imageUrl is String || imageUrl is Int) {
            GlideApp.with(host.applicationContext)
                    .load(imageUrl)
                    .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                    .apply(RequestOptions().error(R.drawable.ic_player))
                    .apply(RequestOptions().circleCrop())
                    .into(imageView)
        } else {
            imageView.visibility = View.GONE
        }
        v.findViewById<View>(R.id.scrim).background = ScrimUtil.makeCubicGradientScrimDrawable(ContextCompat.getColor(host, R.color.scrim),
                5, Gravity.BOTTOM)
        confirmLogin.view = v
        confirmLogin.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
        confirmLogin.duration = Toast.LENGTH_LONG
        confirmLogin.show()
        return ConfirmationToastView(host, message, imageUrl, label)
    }

}
