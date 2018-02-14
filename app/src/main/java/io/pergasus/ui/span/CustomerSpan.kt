/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.span

import `in`.uncod.android.bypass.style.TouchableUrlSpan
import android.content.res.ColorStateList
import android.view.View
import android.widget.Toast


/**
 * Project : OnlineShoppingMart
 * Created by Dennis Bilson on Thu at 12:59 AM.
 * Package name : io.pergasus.ui.span
 */

class CustomerSpan(url: String,
                   private val playerName: String,
                   private val playerId: Long,
                   private val playerUsername: String?,
                   textColor: ColorStateList,
                   pressedBackgroundColor: Int) : TouchableUrlSpan(url, textColor, pressedBackgroundColor) {

    override fun onClick(widget: View) {
        super.onClick(widget)
        Toast.makeText(widget.context, "You clicked a link", Toast.LENGTH_SHORT).show()
    }
}
