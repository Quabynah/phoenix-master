/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import io.pergasus.ui.widget.PasswordEntry
import io.pergasus.util.bindView


/** Login for basic users */
class AuthActivity : Activity() {

    private val frame: FrameLayout  by bindView(R.id.frame_login)
    private val container: ViewGroup  by bindView(R.id.container)
    private val login: Button  by bindView(R.id.login)
    private val register: Button  by bindView(R.id.signup)
    private val email: AutoCompleteTextView  by bindView(R.id.email)
    private val password: PasswordEntry  by bindView(R.id.password)
    private val emailFloat: TextInputLayout  by bindView(R.id.email_float_label)
    private val passwordFloat: TextInputLayout  by bindView(R.id.password_float_label)
    private val message: TextView  by bindView(R.id.login_message)
    private val loginFailed: TextView  by bindView(R.id.login_failed_message)

    private var isDismissing = false
    private var isLoginFailed = false

    private lateinit var client: PhoenixClient
    private lateinit var auth: FirebaseAuth
    private var shouldPromptForPermission: Boolean = false
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
        setupAccountAutocomplete()

        //Action for frame layout & login button
        frame.setOnClickListener(dismiss)
        login.setOnClickListener(doLogin)
        register.setOnClickListener({
            startActivity(Intent(this@AuthActivity, RegisterActivity::class.java))
            finishAfterTransition()
        })
        container.setOnClickListener(null)

        email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                login.isEnabled = true
            }
        })

        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                login.isEnabled = true
            }
        })

    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onEnterAnimationComplete() {
        if (shouldPromptForPermission) {
            requestPermissions(arrayOf(Manifest.permission.GET_ACCOUNTS),
                    PERMISSIONS_REQUEST_GET_ACCOUNTS)
            shouldPromptForPermission = false
        }
        email.setOnFocusChangeListener { _, _ -> maybeShowAccounts() }
        maybeShowAccounts()
    }

    private fun maybeShowAccounts() {
        if (email.hasFocus()
                && email.isAttachedToWindow
                && email.adapter != null
                && email.adapter.count > 0) {
            email.showDropDown()
        }
    }

    @SuppressLint("NewApi")
    private fun setupAccountAutocomplete() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            val accounts = AccountManager.get(this).accounts
            val emailSet = HashSet<String>(0)
            for (account in accounts) {
                if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                    emailSet.add(account.name)
                }
            }
            email.setAdapter(ArrayAdapter<String>(this, R.layout.account_dropdown_item, ArrayList<String>(emailSet)))
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS)) {
                requestPermissions(arrayOf(Manifest.permission.GET_ACCOUNTS),
                        PERMISSIONS_REQUEST_GET_ACCOUNTS)
            } else {
                shouldPromptForPermission = true
            }
        }
    }

    //Do login action
    private val doLogin: View.OnClickListener = View.OnClickListener {
        val mail = email.text.toString()
        val pwd = password.text.toString()
        when {
            mail.isEmpty() -> {
                showMessage("Enter a valid email address")
            }
            pwd.isEmpty() -> {
                showMessage("Enter a valid password")
            }
            else -> {
                showLoading()
                auth.signInWithEmailAndPassword(mail, pwd)
                        .addOnCompleteListener { task ->
                            if (task.isComplete) {
                                val firebaseUser = auth.currentUser
                                updateUI(firebaseUser)
                            } else {
                                showLoginFailed(task.exception?.localizedMessage)
                            }
                        }.addOnFailureListener { exception ->
                            showLoginFailed(exception.localizedMessage)
                        }
            }
        }
    }

    private fun showMessage(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
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
                        //Push map to db
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
        register.visibility = View.VISIBLE
        emailFloat.visibility = View.VISIBLE
        passwordFloat.visibility = View.VISIBLE
        message.visibility = View.VISIBLE
        login.visibility = View.VISIBLE
        loading.hide()
    }

    private fun showLoading() {
        TransitionManager.beginDelayedTransition(container)
        register.visibility = View.GONE
        emailFloat.visibility = View.GONE
        passwordFloat.visibility = View.GONE
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        if (requestCode == PERMISSIONS_REQUEST_GET_ACCOUNTS) {
            TransitionManager.beginDelayedTransition(container)
            if (grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAccountAutocomplete()
                email.requestFocus()
                email.showDropDown()
            }
        }
    }


    companion object {
        private const val STATE_LOGIN_FAILED: String = "STATE_LOGIN_FAILED"
        private const val PERMISSIONS_REQUEST_GET_ACCOUNTS = 2018
    }

}
