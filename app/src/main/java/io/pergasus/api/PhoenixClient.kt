/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.location.Location
import android.net.*
import android.text.TextUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.pergasus.R
import io.pergasus.data.Customer
import io.pergasus.util.ShortcutHelper
import timber.log.Timber


/**
 * Class for handling utilities
 */
class PhoenixClient(val context: Context) {
    //Firebase
    var db: FirebaseFirestore
    var auth: FirebaseAuth
    var storage: StorageReference
        internal set
    private val prefs: SharedPreferences
    private var loginStatusListeners: MutableList<AppLoginStatusListener>? = null
    //Gets the login status of the current user
    var isLoggedIn: Boolean = false
    var isConnected: Boolean = false
    private var loading: MaterialDialog

    private var userId: Long = 0
    private var username: String? = ""
    private var userAvatar: String? = ""
    private var info: String? = ""
    private var customerKey: String? = ""
    private var addressLat: String? = ""
    private var addressLng: String? = ""
    private var token: String? = ""
    /** User's place ID */
    private var place: String? = ""

    /** Current Customer */
    val customer: Customer
        get() = Customer.Builder()
                .setPhoto(userAvatar!!)
                .setName(username!!)
                .setInfo(info!!)
                .setKey(customerKey!!)
                .setId(userId)
                .setAddressLat(addressLat!!)
                .setAddressLng(addressLng!!)
                .setTokenId(token!!)
                .build()

    init {
        //Shared preferences is used for persistent storage of keys
        this.prefs = context.getSharedPreferences(CLIENT_PREFS, Context.MODE_PRIVATE)

        //Initialize Firebase App & Firebase Firestore
        val app = FirebaseApp.initializeApp(context)
        db = if (app != null) FirebaseFirestore.getInstance(app) else FirebaseFirestore.getInstance()
//        Setup Analytics as well...
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance().getReference("phoenix") //Phoenix reference

        //Add properties to shared preferences
        customerKey = prefs.getString(KEY_CUST_UID, null)
        place = prefs.getString(KEY_CUSTOMER_PLACE, null)

        isConnected = getConnectionStatus()

        isLoggedIn = customerKey != null
                && !TextUtils.isEmpty(customerKey)
                && auth.currentUser != null

        if (isLoggedIn) {
            userId = prefs.getLong(KEY_USER_ID, 0L)
            username = prefs.getString(KEY_CUST_NAME, "")
            userAvatar = prefs.getString(KEY_CUST_AVATAR, "")
            info = prefs.getString(KEY_CUST_INFO, "")
            addressLat = prefs.getString(KEY_CUSTOMER_ADDRESS_LAT, "")
            addressLng = prefs.getString(KEY_CUSTOMER_ADDRESS_LNG, "")
            token = prefs.getString(KEY_CUSTOMER_TOKEN, "")
            place = prefs.getString(KEY_CUSTOMER_PLACE, "")
        }

        loading = MaterialDialog.Builder(context)
                .theme(Theme.DARK)
                .progress(true, 0)
                .content(context.getString(R.string.please_wait))
                .canceledOnTouchOutside(false)
                .typeface(Typeface.createFromAsset(context.assets, "fonts/nunito_semibold.ttf"),
                        Typeface.createFromAsset(context.assets, "fonts/nunito_semibold.ttf"))
                .build()
    }

    private var connected = false
    private fun getConnectionStatus(): Boolean {
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        connected = activeNetworkInfo != null && activeNetworkInfo.isConnected

        //Add callback for network connection
        try {
            connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onLost(network: Network?) {
                            connected = false
                        }

                        override fun onAvailable(network: Network?) {
                            connected = true
                        }
                    })
        } catch (e: RuntimeException) {
            Timber.e(e, e.localizedMessage)
        }
        return connected
    }

    /** Listens for the login state of the user */
    interface AppLoginStatusListener {
        fun onUserLogin()
        fun onUserLogout()
    }

    /** Adds [AppLoginStatusListener] */
    fun addLoginStatusListener(listener: AppLoginStatusListener) {
        if (loginStatusListeners == null) {
            loginStatusListeners = ArrayList(0)
        }
        loginStatusListeners!!.add(listener)
    }

    /** Removes [AppLoginStatusListener] */
    fun removeLoginStatusListener(listener: AppLoginStatusListener) {
        if (loginStatusListeners != null) {
            loginStatusListeners!!.remove(listener)
        }
    }

    private fun dispatchLoginEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onUserLogin()
            }
        }
    }

    private fun dispatchLogoutEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onUserLogout()
            }
        }
    }

    /** Set logged in customer instance
     * @param customer
     * */
    fun setLoggedInUser(customer: Customer?) {
        if (customer != null) {
            userId = customer.id
            username = customer.name
            userAvatar = customer.photo
            info = customer.info
            addressLat = customer.addressLat
            addressLng = customer.addressLng
            token = customer.tokenId

            //Save data locally
            val editor = prefs.edit()
            editor.putLong(KEY_USER_ID, userId)
            editor.putString(KEY_CUST_NAME, username)
            editor.putString(KEY_CUST_AVATAR, userAvatar)
            editor.putString(KEY_CUST_INFO, info)
            editor.putString(KEY_CUSTOMER_ADDRESS_LAT, addressLat)
            editor.putString(KEY_CUSTOMER_ADDRESS_LNG, addressLng)
            editor.putString(KEY_CUSTOMER_TOKEN, token)
            editor.apply()

            //Set key
            setCustomer(customer.key!!)
        }
    }

    /**
     * @param customerAccessKey Sets the customer key for the current user in order to track order and add items to cart
     */
    private fun setCustomer(customerAccessKey: String) {
        if (!TextUtils.isEmpty(customerAccessKey)) {
            customerKey = customerAccessKey
            prefs.edit().putString(KEY_CUST_UID, customerKey).apply()
            isLoggedIn = true
            dispatchLoginEvent()
            ShortcutHelper.enableShowCart(context)
        }
    }

    /**
     * @param addressLat   address latitude
     * @param addressLng   address longitude
     */
    fun setAddress(addressLat: String, addressLng: String) {
        if (!isLoggedIn) return
        this.addressLat = addressLat
        this.addressLng = addressLng
        prefs.edit()
                .putString(KEY_CUSTOMER_ADDRESS_LAT, addressLat)
                .putString(KEY_CUSTOMER_ADDRESS_LNG, addressLng)
                .apply()
    }

    /** Set user's current place */
    fun setPlace(address: String?) {
        if (address != null && address.isNotEmpty()) place = address
        prefs.edit().putString(KEY_CUSTOMER_PLACE, place).apply()
    }

    /** Returns user's address */
    fun getPlace(): String? {
        return this.place
    }

    /**
     * Logout current user
     */
    fun logout() {
        auth.signOut()
        // user is now signed out
        isLoggedIn = false
        customerKey = ""
        username = ""
        userAvatar = ""
        info = ""
        addressLat = ""
        addressLng = ""
        token = ""
        val editor = prefs.edit()
        editor.putLong(KEY_USER_ID, 0L)
        editor.putString(KEY_CUST_NAME, "")
        editor.putString(KEY_CUST_AVATAR, "")
        editor.putString(KEY_CUST_UID, "")
        editor.putString(KEY_CUST_INFO, "")
        editor.putString(KEY_CUSTOMER_ADDRESS_LAT, "")
        editor.putString(KEY_CUSTOMER_ADDRESS_LNG, "")
        editor.putString(KEY_CUSTOMER_TOKEN, "")
        editor.apply()
        dispatchLogoutEvent()
        ShortcutHelper.disableShowCart(context)
    }

    /**
     * @param uri   image uri from intent
     */
    fun setImage(uri: Uri) {
        if (!isLoggedIn) return
        userAvatar = uri.toString()
        prefs.edit().putString(KEY_CUST_AVATAR, userAvatar).apply()
    }

    /** Set username with
     * @param name
     * */
    fun setName(name: String) {
        if (!isLoggedIn) return
        username = name
        prefs.edit().putString(KEY_CUST_NAME, username).apply()
    }

    /** Set user info with
     * @param info
     * */
    fun setInfo(info: String) {
        if (!isLoggedIn) return
        this.info = info
        prefs.edit().putString(KEY_CUST_INFO, this.info).apply()
    }

    /** Calculates the distance between two points
     * @param current   Current location of the user
     * @param other     Destination
     * */
    fun calculateDistance(current: Location, other: Location): Double {
        val theta = current.longitude - other.longitude
        var dist = Math.sin(degToRad(current.latitude)).times(Math.sin(degToRad(other.latitude)))
                .times(Math.cos(degToRad(current.latitude))).times(Math.cos(degToRad(other.latitude)))
                .times(Math.cos(degToRad(theta)))
        dist = Math.acos(dist)
        dist = radToDeg(dist)
        dist = dist.times(60).times(1.1515)
        return dist
    }

    /**
     * Converts radians to degrees
     */
    private fun radToDeg(rad: Double): Double {
        return (rad.times(180.0 / Math.PI))
    }

    /**
     * Converts degrees to radians
     */
    private fun degToRad(deg: Double): Double {
        return (deg.times(Math.PI / 180.0))
    }

    /**
     * Returns loading dialog
     */
    fun getDialog(): MaterialDialog {
        return loading
    }

    /**
     * No internet state
     */
    fun showNoNetwork() {
        MaterialDialog.Builder(context)
                .theme(Theme.DARK)
                .content(context.getString(R.string.no_internet_body))
                .title(context.getString(R.string.no_internet_title))
                .typeface(Typeface.createFromAsset(context.assets, "fonts/nunito_semibold.ttf")
                        , Typeface.createFromAsset(context.assets, "fonts/nunito_semibold.ttf"))
                .positiveText(context.getString(R.string.wifi))
                .negativeText(context.getString(R.string.mobile_network))
                .neutralText(context.getString(R.string.cancel))
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .onPositive({ dialog, _ ->
                    dialog.dismiss()
                    //todo: get wifi settings
                })
                .onNegative({ dialog, _ ->
                    dialog.dismiss()
                    //todo: get network settings
                })
                .onNeutral { dialog, _ ->
                    dialog.dismiss()
                }
                .build().show()
    }

    companion object {
        //Variables
        private const val CLIENT_PREFS = "CLIENT_PREFS"
        private const val KEY_USER_ID = "KEY_USER_ID"
        private const val KEY_CUST_UID = "KEY_CUST_UID"
        private const val KEY_CUST_INFO = "KEY_CUST_INFO"
        private const val KEY_CUST_NAME = "KEY_CUST_NAME"
        private const val KEY_CUST_AVATAR = "KEY_CUST_AVATAR"
        private const val KEY_CUSTOMER_ADDRESS_LAT = "KEY_CUSTOMER_ADDRESS_LAT"
        private const val KEY_CUSTOMER_ADDRESS_LNG = "KEY_CUSTOMER_ADDRESS_LNG"
        private const val KEY_CUSTOMER_TOKEN = "KEY_CUSTOMER_TOKEN"
        private const val KEY_CUSTOMER_PLACE = "KEY_CUSTOMER_PLACE"
    }


}