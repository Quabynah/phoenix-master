/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.customtabs

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Fri at 2:35 AM.
 * Package name : io.shoppingmart.util.customtabs
 */

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Empty service used by the custom tab to bind to, raising the application's importance.
 *
 *
 * Adapted from github.com/GoogleChrome/custom-tabs-client
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return sBinder
    }

    companion object {
        private val sBinder = Binder()
    }
}
