/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.iid.FirebaseInstanceId
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Customer
import io.pergasus.ui.transitions.FabTransform
import io.pergasus.ui.transitions.MorphTransform
import io.pergasus.ui.widget.ConfirmationToastView
import io.pergasus.util.bindView


/** Login for basic users */
class AuthActivity : Activity() {

    private val frame: FrameLayout  by bindView(R.id.frame_login)
    private val container: ViewGroup  by bindView(R.id.container)
    private val login: Button  by bindView(R.id.login)
    private val message: TextView  by bindView(R.id.login_message)
    private val loginFailed: TextView  by bindView(R.id.login_failed_message)

    private var isDismissing = false
    private var isLoginFailed = false

    private lateinit var client: PhoenixClient
    private lateinit var auth: FirebaseAuth
    private lateinit var loading: MaterialDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        if (!FabTransform.setup(this, container)) {
            MorphTransform.setup(this, container,
                    ContextCompat.getColor(this, R.color.background_light),
                    resources.getDimensionPixelSize(R.dimen.dialog_corners))
        }

        //Shared preferences
        client = PhoenixClient(this@AuthActivity)
        auth = client.auth

        loading = client.getDialog()

        //Action for frame layout & login button
        frame.setOnClickListener(dismiss)
        login.setOnClickListener(doLogin)
        container.setOnClickListener(null)

    }

    //Do login action
    private val doLogin: View.OnClickListener = View.OnClickListener {
        if (client.isConnected) {
            //Launch AuthUI for Firebase
            showLoading()
            PhoenixUtils.firebaseAuthUI(this@AuthActivity, RC_AUTH_FIREBASE)
        } else {
            showLoginFailed(getString(R.string.no_internet_body))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_AUTH_FIREBASE -> when (resultCode) {
                RESULT_OK -> {
                    val firebaseUser = auth.currentUser
                    updateUI(firebaseUser)
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> showLoginFailed("Cancelled operation")
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            client.db.document("phoenix/mobile")
                    .collection(PhoenixUtils.CUSTOMER_REF)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            //If no data exists in the db, create a new user
                            createNewUser(currentUser)
                        } else {
                            //Start a O(n2) query for all documents in the db
                            for (item in querySnapshot.documents) {
                                //If document snapshot's key is similar to the account id then
                                // log user in else create a new instance of the user
                                if (item.exists() && item.id == currentUser.uid) {
                                    val customer = item.toObject(Customer::class.java)
                                    client.setCustomer(customerAccessKey = customer.key)
                                    client.setLoggedInUser(customer)
                                    if (client.isLoggedIn) {
                                        isLoginFailed = false
                                        val toast = ConfirmationToastView(applicationContext,
                                                client.customer.name,
                                                client.customer.photo,
                                                getString(R.string.app_logged_in_as))
                                        toast.create()
                                        setResult(Activity.RESULT_OK)
                                        finishAfterTransition()
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                        }
                    }
        }
    }

    private fun createNewUser(user: FirebaseUser) {
        //Build new customer instance
        val customer = Customer.Builder()
                .setKey(user.uid)
                .setInfo(user.email!!)
                .setName(if (user.displayName != null) user.displayName!! else "No Username")
                .setPhoto(if (user.photoUrl != null) user.photoUrl?.toString()!! else "default")
                .setId(System.currentTimeMillis())
                .setAddressLat("")
                .setAddressLng("")
                .setTokenId(FirebaseInstanceId.getInstance().token!!)
                .build()

        //Add data to database
        client.db.document("phoenix/mobile")
                .collection(PhoenixUtils.CUSTOMER_REF)
                .document(customer.key!!)
                .set(customer.toHashMap(customer))
                .addOnFailureListener { e ->
                    showLoginFailed(e.localizedMessage)
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        client.setCustomer(customerAccessKey = customer.key)
                        client.setLoggedInUser(customer)
                        if (client.isLoggedIn) {
                            isLoginFailed = false
                            val toast = ConfirmationToastView(applicationContext,
                                    client.customer.name,
                                    client.customer.photo,
                                    getString(R.string.app_logged_in_as))
                            toast.create()
                            setResult(Activity.RESULT_OK)
                            finishAfterTransition()

                        }
                    } else {
                        showLoginFailed(task.exception?.message)
                    }
                }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean(STATE_LOGIN_FAILED, isLoginFailed)
    }

    //Dismiss activity
    private val dismiss: View.OnClickListener = View.OnClickListener {
        isDismissing = true
        setResult(Activity.RESULT_CANCELED)
        finishAfterTransition()
    }

    private fun showLoginFailed(info: String?) {
        isLoginFailed = true
        showLogin()
        loginFailed.visibility = View.VISIBLE
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
    }

    private fun showLogin() {
        TransitionManager.beginDelayedTransition(container)
        message.visibility = View.VISIBLE
        login.visibility = View.VISIBLE
        loading.hide()
    }

    private fun showLoading() {
        TransitionManager.beginDelayedTransition(container)
        message.visibility = View.GONE
        login.visibility = View.GONE
        loginFailed.visibility = View.GONE
        loading.show()
    }

    /** Returns the activity to its previous state */
    override fun onBackPressed() {
        isDismissing = true
        setResult(Activity.RESULT_CANCELED)
        finishAfterTransition()
    }


    companion object {
        private const val STATE_LOGIN_FAILED: String = "STATE_LOGIN_FAILED"
        private const val RC_AUTH_FIREBASE: Int = 34234
    }

}
