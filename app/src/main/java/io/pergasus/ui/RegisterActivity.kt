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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.transition.TransitionManager
import android.util.Patterns
import android.view.ViewGroup
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.StorageReference
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Customer
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.ui.widget.ConfirmationToastView
import io.pergasus.ui.widget.PasswordEntry
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import timber.log.Timber
import java.util.*

class RegisterActivity : Activity() {
    //Widgets
    private val frame: FrameLayout  by bindView(R.id.frame_register)
    private val container: ViewGroup  by bindView(R.id.container)
    private val register: Button  by bindView(R.id.signup)
    private val username: AutoCompleteTextView  by bindView(R.id.username)
    private val email: AutoCompleteTextView  by bindView(R.id.email)
    private val password: PasswordEntry  by bindView(R.id.password)
    private val usernameFloat: TextInputLayout  by bindView(R.id.username_float_label)
    private val emailFloat: TextInputLayout  by bindView(R.id.email_float_label)
    private val passwordFloat: TextInputLayout  by bindView(R.id.password_float_label)
    private val message: TextView  by bindView(R.id.login_message)
    private val image: CircularImageView by bindView(R.id.register_avatar)
    private val address: TextView by bindView(R.id.user_register_address)


    private lateinit var prefs: PhoenixClient
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: StorageReference
    private var imageUri: Uri? = Uri.EMPTY
    private var userAddressLat: String? = null
    private var userAddressLng: String? = null
    private var shouldPromptForPermission: Boolean = false

    private lateinit var loading: MaterialDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Init shared preferences and firebase auth
        prefs = PhoenixClient(this@RegisterActivity)
        auth = prefs.auth
        storage = prefs.storage

        loading = prefs.getDialog()

        setupAccountAutocomplete()

        image.setOnClickListener({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this@RegisterActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) pickImage()
                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            STORAGE_REQUEST_CODE)
                }
            } else pickImage()

        })

        register.setOnClickListener({
            val name = username.text.toString()
            val mail = email.text.toString()
            val pwd = password.text.toString()
            when {
                name.isEmpty() || mail.isEmpty() || pwd.isEmpty() -> {
                    Snackbar.make(frame, "Fill in all required fields first", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    if (prefs.isConnected) {
                        showLoading()
                        auth.createUserWithEmailAndPassword(mail, pwd).addOnFailureListener { exception ->
                            showLoginFailed(exception.localizedMessage)
                        }.addOnCompleteListener { task ->
                                    if (task.isComplete) {
                                        registerUser(name)
                                    } else {
                                        showLoginFailed(task.exception?.localizedMessage)
                                    }
                                }
                    } else {
                        prefs.showNoNetwork()
                    }

                }
            }
        })

        address.setOnClickListener({
            if (ContextCompat.checkSelfPermission(this@RegisterActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Construct an intent for the place picker
                try {
                    val intentBuilder = PlacePicker.IntentBuilder()
                    val intent = intentBuilder.build(this@RegisterActivity)
                    // Start the intent by requesting a result,
                    // identified by a request code.
                    startActivityForResult(intent, PLACE_PICKER_INTENT_RESULT)
                } catch (e: GooglePlayServicesRepairableException) {
                    //show exception
                    Timber.e(e, e.localizedMessage)
                    Toast.makeText(this, "An error occurred : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } catch (e: GooglePlayServicesNotAvailableException) {
                    //show exception
                    Timber.e(e, e.localizedMessage)
                    Toast.makeText(this, "An error occurred : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_REQUEST_CODE)
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

    private fun pickImage() {
        val galleryIntent = Intent()
        galleryIntent.type = "image/*"
        galleryIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(galleryIntent, "Select picture"), IMAGE_REQUEST_CODE)
    }

    private fun registerUser(name: String) {
        var uri: Uri?
        if (imageUri != null && imageUri != Uri.EMPTY) {
            //Upload image to storage bucket
            storage.child(auth.currentUser!!.uid + ".jpg").putFile(imageUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        if (taskSnapshot.task.isComplete) {
                            uri = taskSnapshot.downloadUrl
                            if (uri != null) {
                                updateUI(name, uri!!)
                            } else {
                                showLoginFailed("Image data is null")
                            }
                        } else {
                            showLoginFailed(taskSnapshot.task.exception?.localizedMessage)
                        }
                    }

        } else {
            updateUI(name, Uri.EMPTY)
        }
    }

    private fun updateUI(name: String, uri: Uri) {
        val user: FirebaseUser = auth.currentUser!!
        //Build new customer instance
        val customer = Customer.Builder()
                .setKey(user.uid)
                .setInfo("No status set")
                .setName(name)
                .setPhoto(uri.toString())
                .setId(System.currentTimeMillis())
                .setAddressLat(if (userAddressLat.isNullOrEmpty()) "" else userAddressLat!!)
                .setAddressLng(if (userAddressLng.isNullOrEmpty()) "" else userAddressLng!!)
                .setTokenId(FirebaseInstanceId.getInstance().token!!)
                .build()

        //Add data to database
        prefs.db.document("phoenix/mobile")
                .collection(PhoenixUtils.CUSTOMER_REF)
                .document(customer.key!!)
                .set(customer.toHashMap(customer))
                .addOnFailureListener { e ->
                    showLoginFailed(e.localizedMessage)
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //Push map to db
                        prefs.setLoggedInUser(customer)
                        if (prefs.isLoggedIn) {
                            val toast = ConfirmationToastView(applicationContext,
                                    prefs.customer.name,
                                    prefs.customer.photo,
                                    getString(R.string.app_logged_in_as))
                            toast.create()
                            setResult(Activity.RESULT_OK)
                            finishAfterTransition()
                            return@addOnCompleteListener
                        }
                    } else {
                        showLoginFailed(task.exception?.message)
                    }
                }
    }

    private fun showLoginFailed(info: String?) {
        showLogin()
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
    }

    private fun showLogin() {
        TransitionManager.beginDelayedTransition(frame)
        loading.show()
        register.isEnabled = true
        image.isEnabled = true
        username.isEnabled = true
        email.isEnabled = true
        password.isEnabled = true
    }

    private fun showLoading() {
        TransitionManager.beginDelayedTransition(frame)
        loading.dismiss()
        register.isEnabled = false
        image.isEnabled = false
        username.isEnabled = false
        email.isEnabled = false
        password.isEnabled = false
    }

    override fun onBackPressed() {
        finishAfterTransition()
        startActivity(Intent(applicationContext, AuthActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    imageUri = data?.data
                    //Load image into view with glide
                    GlideApp.with(applicationContext)
                            .asBitmap()
                            .load(imageUri)
                            .apply(RequestOptions().circleCrop())
                            .apply(RequestOptions().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                            .into(image)

                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {/*do nothing*/
                }
            }
        } else if (requestCode == PLACE_PICKER_INTENT_RESULT) {
            when (resultCode) {
                RESULT_OK -> {
                    if (data != null) {
                        val place = PlacePicker.getPlace(this, data)    //Get place from data
                        userAddressLat = place.latLng.latitude.toString()   //Lat
                        userAddressLng = place.latLng.longitude.toString()  //Lng
                        address.text = TextUtils.concat("Your address is: ${place.address}", "\n",
                                "\t(${getString(R.string.setup_address_for_delivery)})")
                    }
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {/*do nothing*/
                }
            }
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

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        if (requestCode == PERMISSIONS_REQUEST_GET_ACCOUNTS) {
            TransitionManager.beginDelayedTransition(container)
            if (grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAccountAutocomplete()
                email.requestFocus()
                email.showDropDown()
            }
        } else if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults != null && grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(applicationContext, "Please enable permission to access storage",
                        Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults != null && grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                address.performClick()
            } else {
                val rationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                if (rationale) {
                    Toast.makeText(this, "You need to allow location permission first",
                            Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
    }

    companion object {
        private const val IMAGE_REQUEST_CODE = 12899
        private const val PERMISSIONS_REQUEST_GET_ACCOUNTS = 12900
        private const val PLACE_PICKER_INTENT_RESULT = 12901
        private const val STORAGE_REQUEST_CODE = 12902
        private const val LOCATION_REQUEST_CODE = 12903
    }
}
