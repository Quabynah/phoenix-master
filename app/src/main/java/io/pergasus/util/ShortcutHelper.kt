/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

/**
 * Project : mobimall-master
 * Created by Dennis Bilson on Sat at 12:58 AM.
 * Package name : io.app.util
 */

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import io.pergasus.R
import io.pergasus.ui.CartActivity

/**
 * Helper for working with launcher shortcuts.
 */
object ShortcutHelper {

    private val SEARCH_SHORTCUT_ID = "search"
    private val CART_SHORTCUT_ID = "view_cart"
    private val DYNAMIC_SHORTCUT_IDS = listOf(CART_SHORTCUT_ID)

    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun enableShowCart(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        val intent = Intent(context, CartActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val postShortcut = ShortcutInfo.Builder(context, CART_SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.shortcut_short_label))
                .setLongLabel(context.getString(R.string.shortcut_long_label))
                .setDisabledMessage(context.getString(R.string.shortcut_disabled))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_post))
                .setIntent(intent)
                .build()
        shortcutManager?.addDynamicShortcuts(listOf(postShortcut))
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun disableShowCart(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        shortcutManager?.disableShortcuts(DYNAMIC_SHORTCUT_IDS)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun reportCartUsed(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        shortcutManager?.reportShortcutUsed(CART_SHORTCUT_ID)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun reportSearchUsed(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        shortcutManager?.reportShortcutUsed(SEARCH_SHORTCUT_ID)
    }

}

