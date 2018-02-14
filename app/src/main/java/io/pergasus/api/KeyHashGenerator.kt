/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api


import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * This is a class that generates a key hash for a package & OUTPUTS RESULT in LogCat
 */
object KeyHashGenerator {

    @SuppressLint("PackageManagerGetSignatures", "LogConditional")
            /**
             * @param packageName   Must be of format "com.example.domain.app"
             */
    fun generateKeyHash(packageName: String,host: Activity){
        //Get the package info
        try {
            val packageInfo = host.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)

            //Iterate through the signatures and convert them to BASE 64 format
            for (signature in packageInfo.signatures){
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())

                //Output result
                Log.d("KeyHash for $packageName", Base64.encodeToString(md.digest(),Base64.DEFAULT))
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.d("KeyHashGenerator",e.localizedMessage)
        }catch (e: PackageManager.NameNotFoundException){
            Log.d("KeyHashGenerator",e.localizedMessage)
        }
    }
}

