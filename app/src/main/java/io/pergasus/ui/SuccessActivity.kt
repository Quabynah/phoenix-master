/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.ViewFlipper
import io.pergasus.R
import io.pergasus.util.bindView

/** Status screen for the order transaction */
class SuccessActivity : Activity() {
    private val flipper: ViewFlipper by bindView(R.id.flipper_success)
    private val shopMore: TextView by bindView(R.id.text_shop_more)
    private val trackOrder: TextView by bindView(R.id.text_track_order)
    private val retry: TextView by bindView(R.id.text_try_again)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        //Get Intent extras
        val page = intent.getIntExtra(EXTRA_PAGE, PAGE_SUCCESS)
        flipper.displayedChild = page

        //Navigate to the HomeScreen
        shopMore.setOnClickListener({
            startActivity(Intent(this@SuccessActivity, HomeActivity::class.java))
            setResultAndFinish()
        })

        //Track current order
        trackOrder.setOnClickListener({
            startActivity(Intent(this@SuccessActivity, TrackingActivity::class.java))
            setResultAndFinish()
        })

        //Get back to the OrderActivity
        retry.setOnClickListener({
            setResult(RESULT_CANCELED)
            finishAfterTransition()
        })
    }

    override fun onNavigateUp(): Boolean {
        setResultAndFinish()
        return true
    }

    override fun onBackPressed() {
        setResultAndFinish()
    }

    private fun setResultAndFinish() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }

    companion object {
        val EXTRA_PAGE = SuccessActivity::class.java.name + ".PAGE"
        val PAGE_SUCCESS = 0
        val PAGE_FAIL = 1
    }
}
