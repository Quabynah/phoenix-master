/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.customtabs

/**
 * Project : ShoppingMart
 * Created by Dennis Bilson on Fri at 2:36 AM.
 * Package name : io.shoppingmart.util.customtabs
 */

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession

/**
 * This is a helper class to manage the connection to the Custom Tabs Service and
 *
 *
 * Adapted from github.com/GoogleChrome/custom-tabs-client
 */
class CustomTabActivityHelper {
    private var mCustomTabsSession: CustomTabsSession? = null
    private var mClient: CustomTabsClient? = null
    private var mConnection: CustomTabsServiceConnection? = null
    private var mConnectionCallback: ConnectionCallback? = null

    /**
     * Creates or retrieves an exiting CustomTabsSession
     *
     * @return a CustomTabsSession
     */
    val session: CustomTabsSession?
        get() {
            if (mClient == null) {
                mCustomTabsSession = null
            } else if (mCustomTabsSession == null) {
                mCustomTabsSession = mClient!!.newSession(null)
            }
            return mCustomTabsSession
        }

    /**
     * Binds the Activity to the Custom Tabs Service
     *
     * @param activity the activity to be bound to the service
     */
    fun bindCustomTabsService(activity: Activity) {
        if (mClient != null) return

        val packageName = CustomTabsHelper.getPackageNameToUse(activity) ?: return
        mConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                mClient = client
                mClient!!.warmup(0L)
                if (mConnectionCallback != null) mConnectionCallback!!.onCustomTabsConnected()
                //Initialize a session as soon as possible.
                session
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
                if (mConnectionCallback != null) mConnectionCallback!!.onCustomTabsDisconnected()
            }
        }
        CustomTabsClient.bindCustomTabsService(activity, packageName, mConnection)
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service
     *
     * @param activity the activity that is bound to the service
     */
    fun unbindCustomTabsService(activity: Activity) {
        if (mConnection == null) return
        activity.unbindService(mConnection)
        mClient = null
        mCustomTabsSession = null
    }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
     *
     * @param connectionCallback
     */
    fun setConnectionCallback(connectionCallback: ConnectionCallback) {
        this.mConnectionCallback = connectionCallback
    }

    /**
     * @return true if call to mayLaunchUrl was accepted
     *
     * @see {@link CustomTabsSession.mayLaunchUrl
     */
    fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
        if (mClient == null) return false

        val session = session ?: return false

        return session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected
     */
    interface ConnectionCallback {
        /**
         * Called when the service is connected
         */
        fun onCustomTabsConnected()

        /**
         * Called when the service is disconnected
         */
        fun onCustomTabsDisconnected()
    }

    companion object {

        /**
         * Opens the URL on a Custom Tab if possible; otherwise falls back to opening it via
         * `Intent.ACTION_VIEW`
         *
         * @param activity         The host activity
         * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
         * @param uri              the Uri to be opened
         */
        fun openCustomTab(activity: Activity,
                          customTabsIntent: CustomTabsIntent,
                          uri: Uri) {
            val packageName = CustomTabsHelper.getPackageNameToUse(activity)

            // if we cant find a package name, it means there's no browser that supports
            // Custom Tabs installed. So, we fallback to a view intent
            if (packageName != null) {
                customTabsIntent.intent.`package` = packageName
                customTabsIntent.launchUrl(activity, uri)
            } else {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

}

