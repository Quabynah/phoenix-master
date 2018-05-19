/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import io.pergasus.R
import io.pergasus.api.PhoenixApplication
import io.pergasus.util.onboarding.PaperOnboardingEngine
import io.pergasus.util.onboarding.PaperOnboardingOnRightOutListener
import io.pergasus.util.onboarding.PaperOnboardingPage
import java.util.*


/**
 * A miniature tutorial screen for new installation
 */
class WelcomeActivity : Activity() {
	
	private lateinit var clientState: PhoenixApplication.Companion.PhoenixClientState
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.onboarding_main_layout)
		
		clientState = PhoenixApplication.Companion.PhoenixClientState(this)
		
		//Setup PaperOnBoardingEngine
		val engine = PaperOnboardingEngine(findViewById(R.id.onboardingRootView), getDataForOnBoarding(), applicationContext)
		engine.setOnRightOutListener(object : PaperOnboardingOnRightOutListener {
			override fun onRightOut() {
				// Probably here will be your exit action
				MaterialDialog.Builder(this@WelcomeActivity)
						.content("Start Shopping")
						.theme(Theme.LIGHT)
						.positiveText("OK")
						.onPositive({ dialog, _ ->
							dialog.dismiss()
							clientState.setAppInstalledState(true)
							startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
							finish()
						})
						.canceledOnTouchOutside(false)
						.build().show()
			}
		})
	}
	
	private fun getDataForOnBoarding(): ArrayList<PaperOnboardingPage> {
		// prepare data
		/*val scr1 = PaperOnboardingPage(getString(R.string.ob_header1), getString(R.string.ob_desc1),
				Color.parseColor("#678FB4"), R.drawable.hotels, R.drawable.key)*/
		val scr2 = PaperOnboardingPage(getString(R.string.ob_header2), getString(R.string.ob_desc2),
				Color.parseColor("#678FB4"), R.drawable.banks, R.drawable.wallet)
		val scr3 = PaperOnboardingPage(getString(R.string.ob_header3), getString(R.string.ob_desc3),
				Color.parseColor("#9B90BC"), R.drawable.stores, R.drawable.shopping_cart)
		
		val elements = ArrayList<PaperOnboardingPage>(0)
//        elements.add(scr1)
		elements.add(scr2)
		elements.add(scr3)
		return elements
	}
	
	override fun onBackPressed() {
		MaterialDialog.Builder(this@WelcomeActivity)
				.content("Start Shopping")
				.theme(Theme.LIGHT)
				.positiveText("OK")
				.onPositive({ dialog, _ ->
					dialog.dismiss()
					clientState.setAppInstalledState(true)
					startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
					finish()
				})
				.canceledOnTouchOutside(false)
				.build().show()
		
	}
	
}
