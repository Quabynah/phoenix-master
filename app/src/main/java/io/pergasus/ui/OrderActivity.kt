/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.hubtel.payments.Class.Environment
import com.hubtel.payments.Exception.HubtelPaymentException
import com.hubtel.payments.HubtelCheckout
import com.hubtel.payments.Interfaces.OnPaymentResponse
import com.hubtel.payments.SessionConfiguration
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.ui.transitions.FabTransform
import io.pergasus.ui.transitions.MorphTransform
import io.pergasus.ui.widget.BottomSheet
import io.pergasus.ui.widget.ObservableScrollView
import io.pergasus.util.AnimUtils
import io.pergasus.util.ImeUtils
import io.pergasus.util.bindView
import timber.log.Timber
import java.text.NumberFormat
import java.util.*

/**
 * User orders activity
 */
@SuppressLint("LogConditional")
class OrderActivity : Activity() {

    private val bottomSheet: BottomSheet by bindView(R.id.bottom_sheet)
    private val bottomSheetContent: ViewGroup by bindView(R.id.bottom_sheet_content)
    private val scrollContainer: ObservableScrollView by bindView(R.id.scroll_container)
    private val sheetTitle: TextView by bindView(R.id.title)
    private val checkOut: Button by bindView(R.id.checkout)
    private val loading: ProgressBar by bindView(R.id.loading)

    //Transaction Details
    private val orderTotal: TextView by bindView(R.id.order_total)
    private val orderTax: TextView by bindView(R.id.order_tax_cost)
    private val orderDelivery: TextView by bindView(R.id.order_delivery_cost)
    private val orderSavings: TextView by bindView(R.id.order_savings)
    private val orderMethod: TextView by bindView(R.id.order_payment_method)
    private val orderLocation: TextView by bindView(R.id.order_customer_location)
    private val orderName: TextView by bindView(R.id.order_customer_name)
    private val updateLocation: Button by bindView(R.id.update_location)
    private val updateMethod: Button by bindView(R.id.update_provider)


    private var appBarElevation: Float = 0.0f
    private lateinit var client: PhoenixClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        if (!FabTransform.setup(this, bottomSheetContent)) {
            MorphTransform.setup(this, bottomSheetContent,
                    ContextCompat.getColor(this, R.color.background_light), 0)
        }

        client = PhoenixClient(this@OrderActivity)
        appBarElevation = resources.getDimension(R.dimen.z_app_bar)

        bottomSheet.registerCallback(object : BottomSheet.Callbacks() {
            override fun onSheetDismissed() {
                // After a drag dismiss, finish without the shared element return transition as
                // it no longer makes sense.  Let the launching window know it's a drag dismiss so
                // that it can restore any UI used as an entering shared element
                setResult(RESULT_DRAG_DISMISSED)
                finish()
            }
        })

        scrollContainer.setListener { scrollY ->
            if (scrollY != 0
                    && sheetTitle.translationZ != appBarElevation) {
                sheetTitle.animate()
                        .translationZ(appBarElevation)
                        .setStartDelay(0L)
                        .setDuration(80L)
                        .setInterpolator(AnimUtils.getFastOutSlowInInterpolator
                        (this@OrderActivity))
                        .start()
            } else if (scrollY == 0 && sheetTitle.translationZ == appBarElevation) {
                sheetTitle.animate()
                        .translationZ(0f)
                        .setStartDelay(0L)
                        .setDuration(80L)
                        .setInterpolator(AnimUtils.getFastOutSlowInInterpolator
                        (this@OrderActivity))
                        .start()
            }
        }

        if (isShareIntent()) {
            val intentReader = ShareCompat.IntentReader.from(this)
            sheetTitle.text = intentReader.text
        }

        if (!hasSharedElementTransition()) {
            // when launched from share or app shortcut there is no shared element transition so
            // animate up the bottom sheet to establish the spatial model i.e. that it can be
            // dismissed downward
            overridePendingTransition(R.anim.post_story_enter, R.anim.post_story_exit)
            bottomSheetContent.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            bottomSheetContent.viewTreeObserver.removeOnPreDrawListener(this)
                            bottomSheetContent.translationY = bottomSheetContent.height.toFloat()
                            bottomSheetContent.animate()
                                    .translationY(0f)
                                    .setStartDelay(120L)
                                    .setDuration(240L).interpolator = AnimUtils
                                    .getLinearOutSlowInInterpolator(this@OrderActivity)
                            return false
                        }
                    })
        }

        val intent = intent
        if (intent.hasExtra(EXTRA_CART_TITLE) && intent.hasExtra(EXTRA_CART_PRICE)) {
            val price = intent.getDoubleExtra(EXTRA_CART_PRICE, 0.0)
            val title = intent.getStringExtra(EXTRA_CART_TITLE)
            //val quantity = intent.getDoubleExtra(EXTRA_CART_QUANTITY, 0.00)
            bindDetails(price, title)
        }

        //Dismiss sheet when user clicks outside it
        bottomSheet.setOnClickListener { dismiss() }
    }

    //Setup details from intent data
    private fun bindDetails(price: Double?, title: String?) {
        if (price == null || title == null) return
        if (BuildConfig.DEBUG) Timber.d("Price is: $price and title is: $title")
        checkOut.isEnabled = true
        checkOut.setOnClickListener {
            if (client.isLoggedIn) {
                ImeUtils.hideIme(sheetTitle)
                when (orderMethod.text) {
                    getString(R.string.slydepay) -> {
                        doMobileMoneyPayment(price, title)
                    }
                    getString(R.string.pay_with_android_pay) -> {
                        doAndroidPay(price, title)
                    }
                    getString(R.string.visa) -> {
                        doPayPalPayment(price, title)
                    }
                }
            } else {
                val login = Intent(this, AuthActivity::class.java)
                val options = ActivityOptions.makeSceneTransitionAnimation(this, checkOut,
                        getString(R.string.transition_dribbble_login))
                ActivityCompat.startActivityForResult(this@OrderActivity, login, RC_AUTH_PAYMENT,
                        options.toBundle())
            }
        }

        //Add details
        orderDelivery.text = setValue(0.00)
        orderSavings.text = setValue(0.00)
        orderTax.text = setValue(0.00)
        loadUser()

        //get amount from intent value
        var amt: Double = price

        //Update amount with additional costs
        amt += valueOf(orderDelivery)
        amt -= valueOf(orderSavings)
        amt += valueOf(orderTax)

        //Set value for the orderTotal
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        orderTotal.text = setValue(amt)

        //Disable or Enable checkout button
        //setButtonState(amt, title)

        //Add actions to buttons
        updateLocation.setOnClickListener {
            startActivityForResult(Intent(this@OrderActivity, ProfileActivity::class.java)
                    , RESULT_UPDATE_PROFILE)
        }

        //Array of options
        val array = arrayOf(
                getString(R.string.slydepay),
                getString(R.string.pay_with_android_pay),
                getString(R.string.visa)
        )
        updateMethod.setOnClickListener {
            MaterialDialog.Builder(this@OrderActivity)
                    .theme(Theme.LIGHT)
                    .title(getString(R.string.confirm_trans_hint))
                    .positiveText("Ok")
                    .negativeText("Cancel")
                    .onNegative({ dialog, _ ->
                        dialog.dismiss()
                    })
                    .onPositive { dialog, which ->
                        when (which.ordinal) {
                            0, 1, 2 -> {
                                textForMethod(which.name)
                                dialog.dismiss()
                            }
                        }
                    }
                    .items(*array)
                    .itemsCallbackSingleChoice(0, { dialog, _, which, text ->
                        when (which) {
                            0, 1, 2 -> {
                                textForMethod(text.toString())
                                dialog.dismiss()
                            }
                        }
                        return@itemsCallbackSingleChoice true
                    })
                    .build().show()
        }

    }

    private fun loadUser() {
        if (client.isLoggedIn) {
            TransitionManager.beginDelayedTransition(bottomSheetContent)
            orderName.text = client.customer.name
            orderLocation.text = client.getPlace()
        }
    }

    //Pay with [PayPal]
    private fun doPayPalPayment(price: Double, title: String?) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        checkOut.isEnabled = false
        //todo: add paypal support
    }

    //Pay with [AndroidPay]
    private fun doAndroidPay(price: Double, title: String?) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        checkOut.isEnabled = false
        //todo: add android pay support
    }

    //Pay with [Slydepay]
    private fun doMobileMoneyPayment(price: Double, title: String?) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        checkOut.isEnabled = false
        doPaymentHubtel(price, title)
    }

    private fun doPaymentHubtel(price: Double, description: String?) {
        try {
            val config: SessionConfiguration = SessionConfiguration()
                    .Builder()
                    .setSecretKey(getString(R.string.hubtel_secret))
                    .setClientId(getString(R.string.hubtel_client_id))
                    .setEnvironment(Environment.LIVE_MODE)
                    .build()
            val checkout = HubtelCheckout(config)
            checkout.setPaymentDetails(price, description)
            checkout.Pay(this)
            checkout.setOnPaymentCallback(object : OnPaymentResponse {
                override fun onCancelled() {
                    // ...
                    Toast.makeText(applicationContext, "Process cancelled",
                            Toast.LENGTH_LONG).show()
                    checkOut.isEnabled = true
                }

                override fun onFailed(p0: String?, p1: String?) {
                    //...
                    Toast.makeText(applicationContext, "Error: $p0", Toast.LENGTH_LONG).show()
                    checkOut.isEnabled = true
                }

                override fun onSuccessful(p0: String?) {
                    //...
                    setResultAndFinish(price)
                }
            })
        } catch (e: HubtelPaymentException) {
            // ...
            Timber.e(e)
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun textForMethod(text: String) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        orderMethod.text = text
    }

    private fun setResultAndFinish(price: Double?) {
        val intent = Intent()
        intent.putExtra(CartActivity.RESULT_PRICE, price)
        setResult(RESULT_PAYING)
        finishAfterTransition()
    }

    override fun onPause() {
        // customize window animations
        overridePendingTransition(R.anim.post_story_enter, R.anim.post_story_exit)
        super.onPause()
    }

    override fun onBackPressed() {
        dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH_PAYMENT) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "You can now continue with your purchase",
                            Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {
                    //do nothing when authentication is cancelled by user
                }
            }
        } else if (requestCode == RC_PAYMENT) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "Purchase was successful. Clearing cart data",
                            Toast.LENGTH_SHORT).show()
                    if (client.isConnected) clearData()
                }
                RESULT_FIRST_USER, RESULT_CANCELED -> {
                    Toast.makeText(this, "We were unable to complete your purchase",
                            Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == RESULT_UPDATE_PROFILE) {
            when (resultCode) {
                RESULT_OK -> {
                    loadUser()
                }
            }
        }
    }

    private fun clearData() {
        if (client.isLoggedIn) {
            TransitionManager.beginDelayedTransition(bottomSheetContent)
            checkOut.isEnabled = false
            loading.visibility = View.VISIBLE
            client.db.document("${PhoenixUtils.ORDER_REF}/${client.customer.key!!}")
                    .delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Cart emptied",
                                    Toast.LENGTH_LONG).show()
                            TransitionManager.beginDelayedTransition(bottomSheetContent)
                            loading.visibility = View.GONE
                            setResult(RESULT_PAYING)
                            finishAfterTransition()
                        } else {
                            Toast.makeText(this, "Failed to empty cart with error: " +
                                    "${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG).show()
                        }
                    }
        } else {
            Toast.makeText(applicationContext, "Cannot empty your shopping cart",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismiss() {
        if (!hasSharedElementTransition()) {
            bottomSheetContent.animate()
                    .translationY(bottomSheetContent.height.toFloat())
                    .setDuration(160L)
                    .setInterpolator(AnimUtils.getFastOutLinearInInterpolator(this@OrderActivity))
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            finish()
                        }
                    })
        } else {
            finishAfterTransition()
        }
    }

    private fun isShareIntent(): Boolean {
        return intent != null && Intent.ACTION_SEND == intent.action
    }

    private fun hasSharedElementTransition(): Boolean {
        val transition = window.sharedElementEnterTransition
        return transition != null && !transition.targets.isEmpty()
    }

    /**
     * @param value converted from double to string
     */
    private fun setValue(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value)
    }

    /**
     * @param view converts text of TextView to double
     */
    private fun valueOf(view: TextView): Double {
        val value = view.text.toString()
        //Handle exceptions here
        if (TextUtils.isEmpty(value) || !TextUtils.isDigitsOnly(value)) return 0.00
        return value.toDouble()
    }

    companion object {
        const val EXTRA_CART_PRICE = "EXTRA_CART_PRICE"
        const val EXTRA_CART_TITLE = "EXTRA_CART_TITLE"
        const val EXTRA_CART_QUANTITY = "EXTRA_CART_QUANTITY"
        const val RESULT_DRAG_DISMISSED = 19
        const val RESULT_PAYING = 20
        const val RC_AUTH_PAYMENT = 21
        const val RC_PAYMENT = 22
        const val RESULT_UPDATE_PROFILE = 23
    }

}
