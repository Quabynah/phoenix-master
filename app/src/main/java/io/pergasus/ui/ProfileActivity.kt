/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.ui.PlacePicker
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import timber.log.Timber

/** User's profile screen */
class ProfileActivity : Activity(), GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback {


    private val container: ViewGroup by bindView(R.id.container)
    private val emailContainer: ViewGroup by bindView(R.id.container_email)
    private val addressContainer: ViewGroup by bindView(R.id.container_address)
    private val favContainer: ViewGroup by bindView(R.id.container_favorite)
    private val loading: ProgressBar by bindView(R.id.loading)
    private val name: TextView by bindView(R.id.customer_name)
    private val about: TextView by bindView(R.id.customer_bio)
    private val avatar: CircularImageView by bindView(R.id.avatar)
    private val email: TextView by bindView(R.id.customer_email)
    private val address: TextView by bindView(R.id.customer_address)
    private val key: TextView by bindView(R.id.customer_key)
    private val save: Button by bindView(R.id.profile_save)

    private lateinit var prefs: PhoenixClient
    private lateinit var geoDataClient: GeoDataClient
    private lateinit var apiClient: GoogleApiClient
    private lateinit var placeDetectionClient: PlaceDetectionClient
    private var imageUri: Uri? = null
    private var placeAddressLat: String? = null
    private var placeAddressLng: String? = null
    private var addressPlace: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        prefs = PhoenixClient(this)

        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this, null)
        // Construct a PlaceDetectionClient.
        placeDetectionClient = Places.getPlaceDetectionClient(this, null)

        apiClient = GoogleApiClient.Builder(this@ProfileActivity)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build()
        //Start using the Places API.

        if (prefs.isLoggedIn) {
            loadUser()
        }
    }

    private fun loadUser() {
        val customer = prefs.customer
        name.text = customer.name
        about.text = customer.info
        email.text = prefs.auth.currentUser?.email
        //Set address here
        address.text = if (prefs.getPlace().isNullOrEmpty())
            getString(R.string.no_address)
        else prefs.getPlace()
        key.text = customer.key

        GlideApp.with(this)
                .load(customer.photo)
                .apply(RequestOptions().circleCrop())
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .apply(RequestOptions().fallback(R.drawable.ic_player))
                .apply(RequestOptions().error(R.drawable.avatar_placeholder))
                .apply(RequestOptions().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(avatar)

        //Edit content
        about.setOnClickListener {
            val builder = AlertDialog.Builder(this@ProfileActivity)
            val view: View = layoutInflater.inflate(R.layout.edit_profile, null, false)
            val textField = view.findViewById<EditText>(R.id.edt_field)
            textField.setText(about.text.toString())
            textField.selectAll()
            builder.setView(view)
            builder.setPositiveButton("Done") { p0, _ ->
                val s = textField.text.toString()
                p0?.dismiss()
                about.text = s
                save.isEnabled = true
            }.setNegativeButton("Cancel") { p0, _ -> p0?.cancel() }
            builder.show()
        }
        name.setOnClickListener {
            val builder = AlertDialog.Builder(this@ProfileActivity)
            val view: View = layoutInflater.inflate(R.layout.edit_profile, null, false)
            val textField = view.findViewById<EditText>(R.id.edt_field)
            textField.setText(name.text.toString())
            textField.selectAll()
            builder.setView(view)
            builder.setPositiveButton("Done") { p0, _ ->
                val s = textField.text.toString()
                p0?.dismiss()
                name.text = s
                save.isEnabled = true
            }.setNegativeButton("Cancel") { p0, _ -> p0?.cancel() }
            builder.show()
        }
        avatar.setOnClickListener({ pickImage() })
        address.setOnClickListener({ pickPlace() })
        save.setOnClickListener {
            showLoading()   //inflate "loading" layout
            if (imageUri != null) {
                uploadImage()
            } else {
                if (prefs.isConnected) {
                    Toast.makeText(this, "Saving changes", Toast.LENGTH_SHORT).show()
                    updateData()
                } else {
                    hideLoading()
                    Toast.makeText(this, "You are not connected to the internet",
                            Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun pickPlace() {
        if (ContextCompat.checkSelfPermission(this@ProfileActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Construct an intent for the place picker
            try {
                val intentBuilder = PlacePicker.IntentBuilder()
                val intent = intentBuilder.build(this@ProfileActivity)
                // Start the intent by requesting a result,
                // identified by a request code.
                startActivityForResult(intent, REQUEST_PLACE_PICKER)
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
    }

    private fun showLoading() {
        TransitionManager.beginDelayedTransition(container)
        loading.visibility = ViewGroup.VISIBLE
        emailContainer.visibility = ViewGroup.GONE
        save.visibility = ViewGroup.GONE
        addressContainer.visibility = ViewGroup.GONE
        favContainer.visibility = ViewGroup.GONE
    }

    private fun hideLoading() {
        TransitionManager.beginDelayedTransition(container)
        loading.visibility = ViewGroup.GONE
        emailContainer.visibility = ViewGroup.VISIBLE
        save.visibility = ViewGroup.VISIBLE
        addressContainer.visibility = ViewGroup.VISIBLE
        favContainer.visibility = ViewGroup.VISIBLE
    }

    private fun updateData() {
        val customer = prefs.customer
        val hashMap = hashMapOf(Pair<String, Any?>("name", name.text.toString()),
                Pair<String, Any?>("about", about.text.toString()),
                Pair<String, Any?>("addressLat", if (placeAddressLat == null) customer.addressLat
                else placeAddressLat),
                Pair<String, Any?>("addressLng", if (placeAddressLng == null) customer.addressLng
                else placeAddressLng)
        )
        //Update data
        prefs.db.document("phoenix/mobile")
                .collection(PhoenixUtils.CUSTOMER_REF)
                .document(prefs.customer.key!!)
                .update(hashMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.setName(name.text.toString())
                        prefs.setAddress(addressLat = placeAddressLat!!, addressLng = placeAddressLng!!)
                        prefs.setPlace(addressPlace)
                        prefs.setInfo(about.text.toString())
                        hideLoading()   //hide "loading" layout
                        save.isEnabled = false
                    }
                }
    }

    private fun uploadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Saving changes", Toast.LENGTH_SHORT).show()
            updateData()
        } else {
            prefs.storage.child(prefs.customer.key + ".jpg").putFile(imageUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        if (taskSnapshot.task.isComplete) {
                            val downloadUrl = taskSnapshot.downloadUrl
                            if (downloadUrl != null) {
                                prefs.setImage(downloadUrl)
                            }
                            if (prefs.isConnected) {
                                prefs.db.document("phoenix/mobile/${PhoenixUtils.CUSTOMER_REF}/${prefs.customer.key}")
                                        .update("photo", downloadUrl.toString())
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(this, "Saving changes", Toast.LENGTH_SHORT).show()
                                                updateData()
                                            } else {
                                                hideLoading()
                                                Toast.makeText(this, task.exception?.localizedMessage,
                                                        Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            hideLoading()
                                            Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
                                        }

                            } else {
                                hideLoading()
                                Toast.makeText(this,
                                        "Upload will be suspended until you are connected to the internet",
                                        Toast.LENGTH_LONG).show()
                            }
                        } else {
                            hideLoading()
                            //Upload was not successful
                            Toast.makeText(this, taskSnapshot.task.exception?.localizedMessage,
                                    Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        hideLoading()
                        Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
                    }
        }
    }

    private fun pickImage() {
        val galleryIntent = Intent()
        galleryIntent.type = "image/*"
        galleryIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(galleryIntent, "Select picture"), IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IMAGE_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    //Image picked
                    val uri = data?.data
                    if (uri != null) {
                        //Load image into resource
                        GlideApp.with(this)
                                .load(uri)
                                .apply(RequestOptions().circleCrop())
                                .apply(RequestOptions().error(R.drawable.avatar_placeholder))
                                .apply(RequestOptions().fallback(R.drawable.ic_player))
                                .apply(RequestOptions().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                                .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(avatar)

                        save.isEnabled = true   //Enable the save button
                        imageUri = uri
                    }
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {/*No image selected by the user*/
                }
            }
            REQUEST_PLACE_PICKER -> when (resultCode) {
                RESULT_OK -> {
                    //Place found
                    // The user has selected a place. Extract the name and address.
                    val place = PlacePicker.getPlace(this, data)
                    val latLng = place.latLng

                    //Set params
                    placeAddressLat = latLng.latitude.toString()
                    placeAddressLng = latLng.longitude.toString()

                    //val name: CharSequence = place.name
                    val address: CharSequence = place.address
                    if (BuildConfig.DEBUG) {
                        Timber.d("User place found to be : $place")
                    }
                    addressPlace = address.toString()
                    this.address.text = addressPlace
                    save.isEnabled = true
                }
                RESULT_CANCELED, RESULT_FIRST_USER -> {/*No place selected by the user*/
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage()
            } else {
                val rationale = ActivityCompat.shouldShowRequestPermissionRationale(this@ProfileActivity, Manifest
                        .permission.WRITE_EXTERNAL_STORAGE)
                if (rationale) {
                    Toast.makeText(this, "You need to allow this permission first", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickPlace()
            } else {
                val rationale = ActivityCompat.shouldShowRequestPermissionRationale(this@ProfileActivity, Manifest
                        .permission.ACCESS_FINE_LOCATION)
                if (rationale) {
                    Toast.makeText(this, "You need to allow this permission first", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        if (BuildConfig.DEBUG) {
            Timber.d(p0.errorMessage)
        } else {
            Toast.makeText(this, p0.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (save.isEnabled) {
            val builder = AlertDialog.Builder(this@ProfileActivity)
            builder.setMessage(getString(R.string.save_changes_prompt))
            builder.setCancelable(false)
            builder.setPositiveButton("Discard", { dialogInterface, _ ->
                dialogInterface.cancel()
                super.onBackPressed()
            })
            builder.setNegativeButton("Save", { dialogInterface, _ ->
                save.performClick()
                dialogInterface.dismiss()
            })
            builder.show()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val REQUEST_STORAGE_CODE = 11
        private const val IMAGE_REQUEST_CODE = 12
        private const val LOCATION_REQUEST_CODE = 13
        private const val REQUEST_PLACE_PICKER = 14
        private val TAG = ProfileActivity::class.java.simpleName
    }
}
