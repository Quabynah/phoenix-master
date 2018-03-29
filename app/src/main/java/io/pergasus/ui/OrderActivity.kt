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
import android.net.Uri
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
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.hubtel.payments.Class.Environment
import com.hubtel.payments.Exception.HubtelPaymentException
import com.hubtel.payments.HubtelCheckout
import com.hubtel.payments.Interfaces.OnPaymentResponse
import com.hubtel.payments.SessionConfiguration
import com.paypal.android.sdk.payments.PayPalConfiguration
import com.paypal.android.sdk.payments.PayPalPayment
import com.paypal.android.sdk.payments.PayPalService
import com.paypal.android.sdk.payments.PaymentActivity
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
import java.math.BigDecimal
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
    private lateinit var walletPaymentSetup: WalletPaymentSetup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        if (!FabTransform.setup(this, bottomSheetContent)) {
            MorphTransform.setup(this, bottomSheetContent,
                    ContextCompat.getColor(this, R.color.background_light), 0)
        }

        client = PhoenixClient(this@OrderActivity)
        appBarElevation = resources.getDimension(R.dimen.z_app_bar)

        //Setup Wallet
        walletPaymentSetup = WalletPaymentSetup(this@OrderActivity, checkOut)

        //Setup PayPal
        setupPayPal()

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

    private fun setupPayPal() {
        val intent = Intent(this, PayPalService::class.java)
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
        startService(intent)
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
                        doAndroidPay(price)
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
        orderDelivery.text = setValue(getRandDelivery())
        orderSavings.text = setValue(getRandSavings())
        orderTax.text = setValue(getRandTax())
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

    private fun getRandDelivery(): Double {
        val random = Random(5)
        return (1.0).plus(random.nextDouble())
    }

    private fun getRandSavings(): Double {
        val random = Random(20)
        return (3.0).plus(random.nextDouble())
    }

    private fun getRandTax(): Double {
        val random = Random(10)
        return random.nextDouble()
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
        performPayPalPayment(price, title)
    }

    private fun performPayPalPayment(price: Double, title: String?) {
        val intent = Intent(this@OrderActivity, PaymentActivity::class.java)
        // send the same configuration for restart resiliency
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
        val thingToBuy = PayPalPayment(BigDecimal(price.toString()), PhoenixUtils.DEF_CURRENCY,
                title, PayPalPayment.PAYMENT_INTENT_SALE)
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy)
        startActivityForResult(intent, REQUEST_CODE_PAYMENT)
    }

    //Pay with [AndroidPay]
    private fun doAndroidPay(price: Double) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        checkOut.isEnabled = false
        performAndroidPay(price)
    }

    private fun performAndroidPay(price: Double) {
        val paymentClient = walletPaymentSetup.getPaymentClient()
        val request = walletPaymentSetup.createPaymentDataRequest(price.toString())
        if (request != null) {
            AutoResolveHelper.resolveTask(paymentClient.loadPaymentData(request),
                    this@OrderActivity, LOAD_PAYMENT_DATA_REQUEST_CODE)
        }
    }

    //Pay with [Slydepay]
    private fun doMobileMoneyPayment(price: Double, title: String?) {
        TransitionManager.beginDelayedTransition(bottomSheetContent)
        checkOut.isEnabled = false
        performPaymentHubtel(price, title)
    }

    private fun performPaymentHubtel(price: Double, description: String?) {
        try {
            val config: SessionConfiguration = SessionConfiguration()
                    .Builder()
                    .setSecretKey(BuildConfig.HUBTEL_CLIENT_SECRET)
                    .setClientId(BuildConfig.HUBTEL_CLIENT_ID)
                    .setEnvironment(HUBTEL_CONFIG_ENVIRONMENT)
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

    override fun onDestroy() {
        // Stop service when done
        stopService(Intent(this, PayPalService::class.java))
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_AUTH_PAYMENT -> when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "You can now continue with your purchase",
                            Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {
                    //do nothing when authentication is cancelled by user
                    checkOut.isEnabled = true
                }
            }
            RC_PAYMENT -> when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "Purchase was successful. Clearing cart data",
                            Toast.LENGTH_SHORT).show()
                    if (client.isConnected) clearData()
                }
                RESULT_FIRST_USER, RESULT_CANCELED -> {
                    Toast.makeText(this, "We were unable to complete your purchase",
                            Toast.LENGTH_LONG).show()
                    checkOut.isEnabled = true
                }
            }
            RESULT_UPDATE_PROFILE -> when (resultCode) {
                RESULT_OK -> {
                    loadUser()
                }
            }
            LOAD_PAYMENT_DATA_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    if (data != null) {
                        val paymentData = PaymentData.getFromIntent(data)
                        val token = paymentData?.paymentMethodToken?.token
                        Toast.makeText(this, "Purchase was successful with token: $token",
                                Toast.LENGTH_SHORT).show()
                        if (client.isConnected) clearData()
                    } else {
                        Toast.makeText(this, "We were unable to retrieve your data",
                                Toast.LENGTH_LONG).show()
                    }
                }
                RESULT_CANCELED, AutoResolveHelper.RESULT_ERROR -> {
                    // Log the status for debugging.
                    // Generally, there is no need to show an error to
                    // the user as the Google Pay API will do that.
                    Toast.makeText(this, "We were unable to complete your purchase",
                            Toast.LENGTH_LONG).show()
                    checkOut.isEnabled = true
                }
            }
        }
    }

    private fun clearData() {
        if (client.isLoggedIn) {
            TransitionManager.beginDelayedTransition(bottomSheetContent)
            checkOut.isEnabled = false
            loading.visibility = View.VISIBLE
            client.db.collection("${PhoenixUtils.ORDER_REF}/${client.customer.key!!}")
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            var complete = false
                            for (doc in task.result.documents) {
                                doc.reference.delete()
                                        .addOnCompleteListener(this@OrderActivity, { delTask ->
                                            Timber.d("${delTask.isComplete} returned")
                                            complete = true
                                        })
                            }

                            if (complete) {
                                TransitionManager.beginDelayedTransition(bottomSheetContent)
                                loading.visibility = View.GONE
                                val intent = Intent()
                                intent.putExtra(CartActivity.RESULT_PRICE, "dummy")
                                setResult(RESULT_PAYING, intent)
                                finishAfterTransition()
                            }

                        } else {
                            Toast.makeText(this, "Failed to empty cart with error: " +
                                    "${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG).show()
                            checkOut.isEnabled = true
                        }
                    }
        } else {
            Toast.makeText(applicationContext, "Cannot empty your shopping cart",
                    Toast.LENGTH_SHORT).show()
            checkOut.isEnabled = true
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
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 24
        private const val REQUEST_CODE_PAYMENT = 25

        /**
         * - Set to PayPalConfiguration.ENVIRONMENT_PRODUCTION to move real money.

         * - Set to PayPalConfiguration.ENVIRONMENT_SANDBOX to use your test credentials
         * from https://developer.paypal.com

         * - Set to PayPalConfiguration.ENVIRONMENT_NO_NETWORK to kick the tires
         * without communicating to PayPal's servers.
         */
        private const val PAYPAL_CONFIG_ENVIRONMENT = PayPalConfiguration.ENVIRONMENT_NO_NETWORK
        private val HUBTEL_CONFIG_ENVIRONMENT = Environment.LIVE_MODE

        private val config = PayPalConfiguration()
                .environment(PAYPAL_CONFIG_ENVIRONMENT)
                .clientId(BuildConfig.PAYPAL_CLIENT_ID)
                .merchantName("The Phoenix")
                .merchantPrivacyPolicyUri(Uri.parse("https://phoenix-master.firebaseapp.com/privacy.html"))
                .merchantUserAgreementUri(Uri.parse("https://phoenix-master.firebaseapp.com/legal.html"))

    }

}
