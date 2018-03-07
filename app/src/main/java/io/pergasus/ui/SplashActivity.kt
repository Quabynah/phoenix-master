/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import io.pergasus.R
import io.pergasus.api.PhoenixApplication
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.util.bindView
import timber.log.Timber

/**
 * SPlash screen for users
 */
class SplashActivity : Activity() {
    private val container: ViewGroup by bindView(R.id.container)
    private val loading: ProgressBar by bindView(R.id.loading)

    private lateinit var prefs: PhoenixClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        prefs = PhoenixClient(this)

        val handler = Handler()
        val clientState = PhoenixApplication.Companion.PhoenixClientState(this)
        if (clientState.isAppRecentlyInstalled) {
            handler.postDelayed({
                if (prefs.isConnected) {
                    if (prefs.isLoggedIn) {
                        TransitionManager.beginDelayedTransition(container)
                        loading.visibility = View.VISIBLE
                        loadUserData()
                    } else {
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                    finish()
                }

            }, 1500)
        } else {
            handler.postDelayed({
                startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                finish()
            }, 2500)
        }
    }

    private fun loadUserData() {
        prefs.db.document("phoenix/mobile/${PhoenixUtils.CUSTOMER_REF}/${prefs.customer.key}")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("User data has been successfully retrieved from the database")
                        loading.visibility = View.GONE
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                    }
                }.addOnFailureListener { exception ->
                    Timber.e(exception, exception.localizedMessage)
                    doLogin()
                }
    }

    private fun doLogin() {
        //Notify user of login state
        MaterialDialog.Builder(this)
                .content("You may not be logged in. Please login to continue")
                .positiveText("Login")
                .negativeText("Exit")
                .typeface(Typeface.createFromAsset(assets, "fonts/nunito_semibold.ttf"),
                        Typeface.createFromAsset(assets, "fonts/nunito_semibold.ttf"))
                .onPositive({ dialog, _ ->
                    dialog.dismiss()
                    loading.visibility = View.GONE
                    startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                })
                .onNegative({ dialog, _ ->
                    dialog.dismiss()
                    finishAfterTransition()
                }).build().show()
    }
}
