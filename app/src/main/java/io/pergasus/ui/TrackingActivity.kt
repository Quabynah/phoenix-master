/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.api.remote.DirectionJSONParser
import io.pergasus.api.remote.IGeoCoordinates
import io.pergasus.data.Purchase
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/**
 * ACTIVITY TO TRACK USER'S PURCHASE ORDER
 * */
@SuppressLint("LogConditional")
class TrackingActivity : Activity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ActivityCompat.OnRequestPermissionsResultCallback {


    private lateinit var _map: GoogleMap
    private var _client: GoogleApiClient? = null
    private var _lastLocation: Location? = null
    private var _locationRequest: LocationRequest? = null
    private lateinit var _coordinates: IGeoCoordinates
    private lateinit var prefs: PhoenixClient
    private lateinit var mapFragment: MapFragment
    private var shouldPromptForPermission: Boolean = false
    private var lat: Double? = null
    private var lng: Double? = null
    private var purchase: Purchase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)
        //Init items
        prefs = PhoenixClient(this)
        //Refer user to login screen for authentication
        if (!prefs.isLoggedIn) {
            finish()
            startActivity(Intent(applicationContext, AuthActivity::class.java))
        }

        //Request user's stored location
        getUserLocation()

        // Obtain the MapFragment and get notified when the map is ready to be used.
        mapFragment = fragmentManager
                .findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        //Initialize GeoCoordinates
        _coordinates = PhoenixUtils.getGeoCoordinates()

        //Request location services if needed
        setupPermissionPrimer()
    }

    private fun getUserLocation() {
        if (prefs.customer.addressLng == null || prefs.customer.addressLat == null) {
            gotoProfileScreen()
        } else {
            lat = prefs.customer.addressLat?.toDouble()
            lng = prefs.customer.addressLng?.toDouble()

            loadData()
        }
    }

    private fun loadData() {
        val intent = intent
        if (intent.hasExtra(EXTRA_PURCHASE)) {
            purchase = intent.getParcelableExtra<Purchase>(EXTRA_PURCHASE)
        }
    }

    private fun gotoProfileScreen() {
        val builder = AlertDialog.Builder(this@TrackingActivity)
        builder.setMessage(getString(R.string.address_prompt))
        builder.setPositiveButton("Profile setup", { dialogInterface, _ ->
            startActivity(Intent(this@TrackingActivity, ProfileActivity::class.java))
            dialogInterface.cancel()
        })
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun setupPermissionPrimer() {
        if (ActivityCompat.checkSelfPermission(this@TrackingActivity, Manifest.permission
                        .ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this@TrackingActivity, Manifest.permission
                        .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            shouldPromptForPermission = true
            ActivityCompat.requestPermissions(this@TrackingActivity,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), RC_LOCATION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == RC_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shouldPromptForPermission = false

                if (checkPlayService()) {
                    buildGoogleApiClient()
                    createLocationRequest()
                }
            } else {
                // if permission was denied check if we should ask again in the future (i.e. they
                // did not check 'never ask again')
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@TrackingActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "Please accept permission to access your location",
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        _map = googleMap
        displayLocation()
    }

    @SuppressLint("MissingPermission")
    private fun displayLocation() {
        if (shouldPromptForPermission) {
            setupPermissionPrimer()
        } else {
            if (_client != null) {
                _lastLocation = LocationServices.FusedLocationApi.getLastLocation(_client)
                if (_lastLocation != null) {
                    //User's stored location
                    val userLocation = LatLng(lat!!, lng!!)
                    val mallLocation = LatLng(5.6227348, -0.1743774)

                    //Set user marker
                    var bitmapUser = BitmapFactory.decodeResource(resources, R.drawable.ic_player)
                    bitmapUser = PhoenixUtils.scaleBitmap(bitmapUser, 70, 70)
                    _map.addMarker(MarkerOptions().position(userLocation).title(prefs.customer.name!!)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmapUser)))

                    //Set mall marker
                    var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shopping_basket_24dp)
                    bitmap = PhoenixUtils.scaleBitmap(bitmap, 70, 70)
                    _map.addMarker(MarkerOptions().position(mallLocation).title("Accra mall")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap)))

                    //Animate camera to user's location
                    //_map.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
                    val cameraPosition = CameraPosition.Builder()
                            .target(userLocation)
                            .tilt(30f)
                            .bearing(90f)
                            .zoom(13f)
                            .build()
                    _map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null)

                    //Draw route to mall location
                    drawRoute(mallLocation, userLocation)
                } else {
                    Toast.makeText(this, "Unable to retrieve user location", Toast.LENGTH_LONG)
                            .show()
                }
            }
        }
    }

    private fun drawRoute(latLng: LatLng, orderLocation: LatLng) {
        //Create bitmap for marker
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shopping_basket_24dp)
        bitmap = PhoenixUtils.scaleBitmap(bitmap, 70, 70)

        //Create a new marker
        val marker = MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .title(
                        if (purchase == null) "Order ${prefs.customer.name}"
                        else "Order ${purchase?.key}"
                )
                .position(orderLocation)

        //Add marker to map
        _map.addMarker(marker)

        //Draw route
        _coordinates.getDirections("${latLng.latitude},${latLng.longitude}",
                "${orderLocation.latitude},${orderLocation.longitude}")
                .enqueue(object : Callback<String?> {
                    override fun onFailure(call: Call<String?>?, t: Throwable?) {
                        Timber.d("drawRoute: getDirections: onFailure called with : ${t?.localizedMessage}")
                        Toast.makeText(this@TrackingActivity,
                                "Failed to get  directions", Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(call: Call<String?>?, response: Response<String?>?) {
                        ParserTask().execute(response?.body().toString())
                    }
                })
    }

    /* @SuppressLint("MissingPermission")
     private fun drawRoute(latLng: LatLng, address: String?) {
         if (address == null || address.isEmpty()) {
             return
         } else {
             _coordinates.getGeoCode(address).enqueue(object : Callback<String?> {
                 override fun onFailure(call: Call<String?>?, t: Throwable?) {
                     Log.d(TAG, "drawRoute: getGeoCode: called with : ${t?.localizedMessage}")
                     Toast.makeText(this@TrackingActivity, "Failed to get your GeoCode",
                             Toast.LENGTH_LONG).show()
                 }

                 override fun onResponse(call: Call<String?>?, response: Response<String?>?) {
                     try {
                         val jsonObject = JSONObject(response?.body().toString())

                         //Get Latitude
                         val lat: String = (jsonObject.get("results") as JSONArray)
                                 .getJSONObject(0)
                                 .getJSONObject("geometry")
                                 .getJSONObject("location")
                                 .get("lat").toString()

                         //Get Longitude
                         val lng: String = (jsonObject.get("results") as JSONArray)
                                 .getJSONObject(0)
                                 .getJSONObject("geometry")
                                 .getJSONObject("location")
                                 .get("lng").toString()

                         //Set Order Location
                         val orderLocation = LatLng(lat.toDouble(), lng.toDouble())

                         //Create bitmap for marker
                         var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shopping_basket_24dp)
                         bitmap = PhoenixUtils.scaleBitmap(bitmap, 70, 70)

                         //Create a new marker
                         val marker = MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                 .title("Order of: ${prefs.customer.name}")
                                 .position(orderLocation)

                         //Add marker to map
                         _map.addMarker(marker)

                         //Draw route
                         _coordinates.getDirections("${latLng.latitude},${latLng.longitude}", address)
                                 .enqueue(object : Callback<String?> {
                                     override fun onFailure(call: Call<String?>?, t: Throwable?) {
                                         Log.d(TAG, "drawRoute: getDirections: onFailure called with : ${t?.localizedMessage}")
                                         Toast.makeText(this@TrackingActivity,
                                                 "Failed to get  directions", Toast.LENGTH_LONG).show()
                                     }

                                     override fun onResponse(call: Call<String?>?, response: Response<String?>?) {
                                         ParserTask().execute(response?.body().toString())
                                     }
                                 })

                     } catch (e: JSONException) {
                         Log.d(TAG, "JsonException arose: ${e.localizedMessage}")
                     }

                 }
             })
         }
     }*/

    private fun checkPlayService(): Boolean {
        val i = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (i != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(i)) {
                GooglePlayServicesUtil.getErrorDialog(i, this, RC_PLAY_SERVICES_RESOLUTION).show()
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show()
                finishAfterTransition()
            }
            return false
        }
        return true
    }

    protected fun buildGoogleApiClient() {
        _client = GoogleApiClient.Builder(this@TrackingActivity, this, this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build()
        _client?.connect()
    }

    private fun createLocationRequest() {
        _locationRequest = LocationRequest()
        _locationRequest?.interval = UPDATE_INTERVAL
        _locationRequest?.fastestInterval = FASTEST_INTERVAL
        _locationRequest?.smallestDisplacement = DISPLACEMENT
        _locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (shouldPromptForPermission) return
        if (_client != null && _locationRequest != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(_client, _locationRequest, this)
        } else {
            Timber.d("startLocationUpdates: Client and Request not initialized properly")
        }
    }

    override fun onStart() {
        super.onStart()
        _client?.connect()
    }

    override fun onStop() {
        _client?.disconnect()
        super.onStop()
    }

    //API OVERRIDES
    override fun onConnected(p0: Bundle?) {
        displayLocation()
        startLocationUpdates()
    }

    override fun onConnectionSuspended(p0: Int) {
        _client?.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Timber.d("OnConnectionFailed: ${p0.errorMessage}")
    }

    override fun onLocationChanged(p0: Location?) {
        //Sets the last location of the current user
        _lastLocation = p0
        displayLocation()
    }

    /**
     * Parse Json for use in google maps
     */
    @SuppressLint("StaticFieldLeak")
    inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        private val dialog: ProgressDialog = ProgressDialog(this@TrackingActivity)

        override fun onPreExecute() {
            super.onPreExecute()
            dialog.setMessage(getString(R.string.need_login))
            dialog.show()
        }

        override fun doInBackground(vararg p0: String?): List<List<HashMap<String, String>>>? {
            val jsonObject: JSONObject?
            var routes: List<List<HashMap<String, String>>>? = null

            try {
                jsonObject = JSONObject(p0[0])
                val parser = DirectionJSONParser()
                routes = parser.parse(jsonObject)
            } catch (e: JSONException) {
                Timber.d("Json exception from ParserTask: doInBackground: ${e.localizedMessage}")
            }

            //Return routes
            return routes
        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
            //Dismiss dialog
            dialog.dismiss()

            //Variables to be used
            var points: ArrayList<LatLng>?
            var lineOptions: PolylineOptions? = null

            //Check result nullity first
            if (result != null) {
                for (i in 0 until result.size) {
                    points = ArrayList(0)
                    lineOptions = PolylineOptions()

                    val path: List<HashMap<String, String>> = result[i]
                    for (j in 0 until path.size) {
                        val point: HashMap<String, String> = path[j]
                        val lat = point["lat"]?.toDouble()
                        val lng = point["lng"]?.toDouble()

                        if (lat != null && lng != null) {
                            val position = LatLng(lat, lng)
                            points.add(position)
                        }

                    }

                    //Add all LatLng to the lineOptions
                    lineOptions.addAll(points)
                    lineOptions.width(12.0f)
                    lineOptions.color(Color.BLUE)
                    lineOptions.geodesic(true)
                }

                //Add Polyline, if any, to map
                _map.addPolyline(lineOptions)
            }

        }
    }

    companion object {
        private const val RC_LOCATION = 3028
        const val EXTRA_PURCHASE = "EXTRA_PURCHASE"
        private const val RC_PLAY_SERVICES_RESOLUTION = 13
        private const val UPDATE_INTERVAL = 1000L
        private const val FASTEST_INTERVAL = 5000L
        private const val DISPLACEMENT = 10.0f
    }
}
