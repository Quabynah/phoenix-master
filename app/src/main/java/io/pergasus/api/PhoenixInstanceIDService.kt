/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.pergasus.BuildConfig

/** [FirebaseInstanceIdService] */
class PhoenixInstanceIDService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        //Get updated token
        val refreshedToken = FirebaseInstanceId.getInstance().token
        if (BuildConfig.DEBUG) Log.d(TAG, refreshedToken)
        sendRegistrationToServer(refreshedToken)
    }

    //todo: send token to server
    private fun sendRegistrationToServer(token: String?) {}

    companion object {
        private val TAG = PhoenixInstanceIDService::class.java.simpleName
    }
}