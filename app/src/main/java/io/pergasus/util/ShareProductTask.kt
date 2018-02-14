/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.AsyncTask
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import android.util.Log
import com.bumptech.glide.Glide
import io.pergasus.BuildConfig
import io.pergasus.data.Product
import java.io.File
import java.lang.StringBuilder


class ShareProductTask(@SuppressLint("StaticFieldLeak") private val activity: Activity, private val product: Product):
        AsyncTask<Void, Void, File>() {

    override fun doInBackground(vararg p0: Void?): File? {
        val url = product.url
        try {
            return Glide
                    .with(activity)
                    .load(url)
                    .downloadOnly(product.colspan, product.colspan)
                    .get()
        } catch (ex: Exception) {
            Log.w("SHARE", "Sharing $url failed", ex)
            return null
        }

    }

    override fun onPostExecute(result: File?) {
        if (result == null) { return; }
        // glide cache uses an unfriendly & extension-less name,
        // massage it based on the original
        var fileName = product.url
        fileName = fileName?.substring(fileName.lastIndexOf('/') + 1)
        val renamed = File(result.parent, fileName)
        result.renameTo(renamed)
        val uri = FileProvider.getUriForFile(activity, BuildConfig.FILES_AUTHORITY, renamed)
        ShareCompat.IntentBuilder.from(activity)
                .setText(getShareText())
                .setType(getImageMimeType(fileName!!))
                .setSubject(product.name)
                .setStream(uri)
                .startChooser()
    }

    private fun getShareText(): String{
        return StringBuilder()
                .append("“")
                .append(product.name)
                .append("” about ")
                .append(product.description)
                .append("\n")
                .append(product.url)
                .toString()
    }

    private fun getImageMimeType(fileName: String): String {
        if (fileName.endsWith(".png")) {
            return "image/png"
        } else if (fileName.endsWith(".gif")) {
            return "image/gif"
        }
        return "image/jpeg"
    }

}