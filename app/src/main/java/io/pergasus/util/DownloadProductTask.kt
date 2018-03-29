/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

import android.app.Activity
import android.os.AsyncTask
import android.widget.Toast
import com.bumptech.glide.Glide
import io.pergasus.BuildConfig
import io.pergasus.data.Product
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadProductTask(private val activity: Activity, private val product: Product) :
        AsyncTask<Void, Void, File>() {

    override fun doInBackground(vararg p0: Void?): File? {
        val url = product.url
        return try {
            Glide.with(activity)
                    .load(url)
                    .downloadOnly(1200, 900)
                    .get()
        } catch (ex: Exception) {
            Timber.w(ex, "Downloading $url failed")
            null
        }

    }

    override fun onPostExecute(result: File?) {
        if (result == null) {
            return
        }

        var fos: FileOutputStream? = null

        var fileName = product.url
        fileName = fileName?.substring(fileName.lastIndexOf('/') + 1)
        val renamed = File(result.parent, fileName)
        result.renameTo(renamed)

        try {
            //Generate output stream
            fos = FileOutputStream(result)
            //Create new file if it doesn't exist
            if (!result.exists()) result.createNewFile()
            //Read file size and return result if it is too large
            if (result.length() > Int.MAX_VALUE) {
                Toast.makeText(activity, "File is too large to be downloaded",
                        Toast.LENGTH_SHORT).show()
                return
            }

            //Get bytes from file
            val bytes = result.readBytes()
            //Write file to storage
            fos.write(bytes)
            fos.flush()
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Timber.d(e)
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
        } finally {
            try {
                if (fos != null) {
                    fos.close()
                }
            } catch (e: IOException) {
                Toast.makeText(activity, "File is too large to be downloaded",
                        Toast.LENGTH_SHORT).show()
                return
            }
        }

    }

}
