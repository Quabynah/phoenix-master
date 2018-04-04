/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.support.multidex.MultiDex
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import io.pergasus.BuildConfig
import timber.log.Timber

/** Phoenix Application */
class PhoenixApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        //Set debugger
        if (BuildConfig.DEBUG) {
            //Timber init
            Timber.plant(Timber.DebugTree())
        }

        //Setup notification service
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("purchases")
            FirebaseMessaging.getInstance().subscribeToTopic("products")
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val USER_PREFS = "USER_PREFS"
        private const val KEY_APP_STATE = "KEY_APP_STATE"

        /**
         * Class to show user state upon app installation
         */
        class PhoenixClientState(context: Context) {
            private var prefs: SharedPreferences = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)

            private var state: Boolean = false
            var isAppRecentlyInstalled = false

            init {
                state = prefs.getBoolean(KEY_APP_STATE, false)
                isAppRecentlyInstalled = state

                if (isAppRecentlyInstalled)
                    state = prefs.getBoolean(KEY_APP_STATE, false)
            }

            fun setAppInstalledState(newState: Boolean) {
                state = newState
                prefs.edit().putBoolean(KEY_APP_STATE, state).apply()
            }
        }
    }
}
