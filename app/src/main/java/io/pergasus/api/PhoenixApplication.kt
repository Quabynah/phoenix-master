/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import timber.log.Timber

/** Phoenix Application */
class PhoenixApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        //Timber init
        Timber.plant(Timber.DebugTree())
    }
}
