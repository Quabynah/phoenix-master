/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.Manifest.permission
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Purchase
import io.pergasus.util.maputil.RouteOverlayView.AnimType
import io.pergasus.util.maputil.TrailSupportMapFragment
import java.util.*


/**
 * Track delivery of Goods in real-time
 */
class TrackingActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener, OnConnectionFailedListener, ConnectionCallbacks {

    private var shouldPromptForPermission: Boolean = false

    private var map: GoogleMap? = null
    private var client: PhoenixClient? = null
    private var apiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null
    private var lastLocation: Location? = null
    private var purchase: Purchase? = null
    private var mapFragment: TrailSupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        //Init shared preferences
        client = PhoenixClient(this)

        //Setup permission primer
        shouldPromptForPermission = hasNoPermission()

        //Request permission once the activity starts
        if (shouldPromptForPermission) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                requestLocationPermission()
            }
        }

        //Get map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as TrailSupportMapFragment
        //Sync map
        mapFragment!!.getMapAsync(this)

        //Get intent data
        val intent = intent
        if (intent.hasExtra(EXTRA_PURCHASE)) {
            purchase = intent.getParcelableExtra(EXTRA_PURCHASE)
            showMessage("You are currently tracking Purchase: " + purchase!!.key!!)
        }
    }

    private fun hasNoPermission(): Boolean {
        //Checks whether user has enabled permission to access their location
        return ContextCompat.checkSelfPermission(this@TrackingActivity,
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@TrackingActivity,
                permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
        apiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build()
        apiClient!!.connect()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest!!.interval = UPDATE_INTERVAL
        locationRequest!!.fastestInterval = FASTEST_INTERVAL
        locationRequest!!.smallestDisplacement = DISPLACEMENT
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (shouldPromptForPermission) return
        if (apiClient != null && locationRequest != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this)
        } else {
            showMessage("Google API client and Location request is not properly setup")
        }
    }

    //Checks the availability of Google Play Services on the current device
    private fun checkPlayService(): Boolean {
        val i = GooglePlayServicesUtil.isGooglePlayServicesAvailable(applicationContext)
        if (i != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(i)) {
                GooglePlayServicesUtil.getErrorDialog(i, this, RC_PLAY_SERVICES_RESOLUTION).show()
            } else {
                showMessage("This device is not supported")
                finishAfterTransition()
            }
            return false
        }

        return true
    }

    private fun showMessage(message: CharSequence?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        //Connect API Client
        if (apiClient != null) apiClient!!.connect()
    }

    override fun onStop() {
        //Disconnect API client
        if (apiClient != null && apiClient!!.isConnected) apiClient!!.disconnect()
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.uiSettings.isRotateGesturesEnabled = true
        map!!.uiSettings.isTiltGesturesEnabled = false
        map!!.setMaxZoomPreference(18f)


        //Set custom map style
        map!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(applicationContext,
                R.raw.zuber_map_style))

        //Callback for map loaded state
        map!!.setOnMapLoadedCallback {
            //Get user's current location
            if (lastLocation == null) {
                if (shouldPromptForPermission) {
                    if (VERSION.SDK_INT >= VERSION_CODES.M) {
                        requestLocationPermission()
                        shouldPromptForPermission = true
                    }
                }
            } else if (checkPlayService()) {
                buildGoogleApiClient()
                createLocationRequest()
                displayLocation()
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun displayLocation() {
        if (shouldPromptForPermission) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                requestLocationPermission()
            }
        } else {
            //Check user's login state. It may have changed before this activity is created
            if (client!!.isLoggedIn) {
                if (apiClient != null) {
                    if (lastLocation == null) {
                        lastLocation = LocationServices.FusedLocationApi
                                .getLastLocation(apiClient)
                    }

                    //Obtain user's location from last known location
                    val userLocation = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                    //Get the Mall's current location
                    val mallGeoPoint = PhoenixUtils.MALL_GEO_POINT

                    //Create Bounds for location
                    val builder = LatLngBounds.Builder()
                    //Add user's location
                    builder.include(userLocation)
                    //Add mall's location
                    builder.include(mallGeoPoint)
                    //Build bounds
                    val bounds = builder.build()
                    //Create camera updater
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 200)

                    //Apply animation to map
                    map!!.moveCamera(cameraUpdate)
                    map!!.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)

                    //Update camera movement with fragment
                    map!!.setOnCameraMoveListener { mapFragment!!.onCameraMove(map) }

                    //Add locations as list
                    val routes = ArrayList<LatLng>(0)
                    routes.add(userLocation)
                    routes.add(mallGeoPoint)

                    //Finally, start animation: Can be replaces with startAnimation(routes);
                    Handler().postDelayed({ mapFragment?.setUpPath(routes, map, AnimType.ARC) }, 1000)

                } else
                    showMessage("Google API client cannot be created")

            }
        }
    }

    private fun startAnimation(routes: List<LatLng>) {
        if (map == null) {
            showMessage("Map is not ready")
        } else {
            MapAnimator.getInstance().animateRoute(map, routes)
        }
    }

    @TargetApi(VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == RC_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shouldPromptForPermission = false
                if (checkPlayService()) {
                    buildGoogleApiClient()
                    createLocationRequest()
                    displayLocation()
                }

            } else if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                showMessage("Please accept permission to access your location")
                requestLocationPermission()
            }
        }
    }

    @RequiresApi(api = VERSION_CODES.M)
    private fun requestLocationPermission() {
        requestPermissions(arrayOf(permission.ACCESS_FINE_LOCATION, permission
                .ACCESS_COARSE_LOCATION), RC_LOCATION)
    }

    override fun onLocationChanged(location: Location) {
        //Set user's current location
        lastLocation = location
        displayLocation()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        //Show message to user
        showMessage(connectionResult.errorMessage)
    }

    override fun onConnected(bundle: Bundle?) {
        //Update location and display routes
        displayLocation()
        startLocationUpdates()
    }

    override fun onConnectionSuspended(i: Int) {
        //Reconnect API client
        apiClient!!.connect()
    }

    companion object {
        //Constants
        const val EXTRA_PURCHASE = "EXTRA_PURCHASE"
        const val RC_PLAY_SERVICES_RESOLUTION = 123
        const val RC_LOCATION = 124
        const val UPDATE_INTERVAL = 1000L   //1.0 sec update interval
        const val FASTEST_INTERVAL = 5000L   //1.0 sec update interval
        const val DISPLACEMENT = 10.0f   //1.0 sec update interval
    }
}
