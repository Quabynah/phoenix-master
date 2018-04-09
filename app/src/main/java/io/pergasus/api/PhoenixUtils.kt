/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.maps.model.LatLng
import io.pergasus.R

/**
 * Utility for handling constant variables used throughout the application
 */
object PhoenixUtils {
    //Firebase Server url
    const val DB_PREFIX = "phoenix"

    //Default currency
    const val DEF_CURRENCY = "USD"

    //Database references
    const val CUSTOMER_REF = "users"
    const val PRODUCTS_REF = "$DB_PREFIX/products"
    //val HISTORY_REF = "history"
    const val COMMENT_REF = "$DB_PREFIX/comments"
    const val ALL_CATEGORY_REF = "all"

    //User's order(s) reference
    const val ORDER_REF = "$DB_PREFIX/orders"

    //User's purchase(s) reference
    const val PURCHASE_REF = "$DB_PREFIX/purchases"

    //Notifications reference
    const val NOTIFICATIONS_REF = "$DB_PREFIX/mobile/notifications"

    //Products references
    const val FAVORITE_REF = "favorites"
    const val BUSINESS_REF = "business"
    const val STUDENT_REF = "students"
    const val KIDS_REF = "kids"
    const val CLOTHING_REF = "clothing"
    const val ENTERTAINMENT_REF = "entertainment"
    const val HEALTH_REF = "health"

    //Shops references
    const val SHOP_REF = "web/shops"

    //Followers references (i.e. users following a particular shop)
    const val FOLLOW_REF = "$DB_PREFIX/followers"
    const val LIKES_REF = "$DB_PREFIX/likes"

    //GeoLocation for Mall
    val MALL_GEO_POINT = LatLng(5.6227348, -0.1743774)

    //Scale Bitmap to preferred size and dimensions
    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        //Create new Bitmap
        val scaledBitmap: Bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

        //Set props
        val scaleX: Float = newWidth.div(bitmap.width.toFloat())
        val scaleY: Float = newHeight.div(bitmap.height.toFloat())
        val pivotX = 0.0f
        val pivotY = 0.0f

        //Setup Matrix
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY)

        //Setup Canvas
        val canvas = Canvas(scaledBitmap)
        canvas.matrix = scaleMatrix
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, Paint(Paint.FILTER_BITMAP_FLAG))

        //Return Bitmap
        return scaledBitmap
    }

    /**
     * Firebase Auth UI class
     */
    fun firebaseAuthUI(host: Activity, requestCode: Int) {
        //Service providers: Email, Google, Phone
        val providers = arrayListOf(
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.phoenix_logo)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
        host.startActivityForResult(intent, requestCode)
    }

}
