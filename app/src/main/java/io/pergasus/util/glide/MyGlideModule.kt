/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.glide

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
open class MyGlideModule : AppGlideModule() {
    @SuppressLint("CheckResult")
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val defaultOptions = RequestOptions()
        val activityManager: ActivityManager? = context.getSystemService(ACTIVITY_SERVICE) as
                ActivityManager

        if (activityManager != null) {
            defaultOptions.format(if (activityManager.isLowRamDevice) DecodeFormat
                    .PREFER_RGB_565 else DecodeFormat.PREFER_ARGB_8888)
            defaultOptions.disallowHardwareConfig()
            builder.setDefaultRequestOptions(defaultOptions)
        }
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

}