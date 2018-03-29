/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.annotation.TargetApi
import android.app.Activity
import android.app.assist.AssistContent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toolbar
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.pergasus.R
import io.pergasus.data.Product
import io.pergasus.ui.widget.ZoomageView
import io.pergasus.util.DownloadProductTask
import io.pergasus.util.ShareProductTask
import io.pergasus.util.bindView
import io.pergasus.util.customtabs.CustomTabActivityHelper
import io.pergasus.util.glide.GlideApp

/**
 * Image details from a product
 */
class ImageDetailsActivity : Activity() {

    private val container: ViewGroup by bindView(R.id.container)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val imageView: ZoomageView by bindView(R.id.imageView)

    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_details)
        setActionBar(toolbar)

        toolbar.setNavigationOnClickListener({
            onBackPressed()
        })

        //Get intent from parent activity
        val intent = intent
        if (intent.hasExtra(EXTRA_IMAGE_URL)) {
            //Get intent data
            product = intent.getParcelableExtra(EXTRA_IMAGE_URL)
            loadImageFromURL()
        }
    }

    private fun loadImageFromURL() {
        if (product != null) {
            //Toolbar title
            toolbar.title = product?.name

            //Load image
            GlideApp.with(this)
                    .load(product!!.url)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .priority(Priority.IMMEDIATE)
                    .override(800, 600)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)

            imageView.setOnClickListener({ openLink(product!!.url!!) })

            //Status bar color
            window.statusBarColor = ContextCompat.getColor(this@ImageDetailsActivity, R.color.black)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_share -> {
                if (product != null) {
                    ShareProductTask(this, product!!).execute()
                }
                true
            }
            R.id.menu_get -> {
                if (product != null) {
                    DownloadProductTask(this, product!!).execute()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    override fun onProvideAssistContent(outContent: AssistContent) {
        outContent.webUri = Uri.parse(product!!.url)
    }

    internal fun openLink(url: String) {
        CustomTabActivityHelper.openCustomTab(
                this@ImageDetailsActivity,
                CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this@ImageDetailsActivity, R.color.dribbble))
                        .addDefaultShareMenuItem()
                        .build(),
                Uri.parse(url))
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }

    companion object {
        const val EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"
    }
}
