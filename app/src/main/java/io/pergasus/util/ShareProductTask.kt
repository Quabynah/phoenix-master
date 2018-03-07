/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.AsyncTask
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import com.bumptech.glide.Glide
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.data.Product
import timber.log.Timber
import java.io.File


class ShareProductTask(@SuppressLint("StaticFieldLeak") private val activity: Activity, private val product: Product) :
        AsyncTask<Void, Void, File>() {

    private val shareText: String
        get() = "“" + product.name + "” by " + product.shop + "\n" + product.url + "\n" +
                "Image generated from ${activity.getString(R.string.app_name)} \nCreated by Dennis Bilson"

    override fun doInBackground(vararg p0: Void?): File? {
        val url = product.url
        return try {
            Glide
                    .with(activity)
                    .load(url)
                    .downloadOnly(1200, 900)
                    .get()
        } catch (ex: Exception) {
            Timber.w(ex, "Sharing $url failed")
            null
        }

    }

    override fun onPostExecute(result: File?) {
        if (result == null) {
            return
        }
        // glide cache uses an unfriendly & extension-less name,
        // massage it based on the original
        var fileName = product.url
        fileName = fileName?.substring(fileName.lastIndexOf('/') + 1)
        val renamed = File(result.parent, fileName)
        result.renameTo(renamed)
        val uri = FileProvider.getUriForFile(activity, BuildConfig.FILES_AUTHORITY, renamed)
        ShareCompat.IntentBuilder.from(activity)
                .setText(shareText)
                .setType(getImageMimeType(fileName))
                .setSubject(product.name)
                .setStream(uri)
                .startChooser()
    }


    private fun getImageMimeType(fileName: String?): String {
        if (fileName.isNullOrEmpty()) return "image/jpeg"
        if (fileName!!.endsWith(".png")) {
            return "image/png"
        } else if (fileName.endsWith(".gif")) {
            return "image/gif"
        }
        return "image/jpeg"
    }

}