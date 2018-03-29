/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.ArrayList;
import java.util.List;

import io.pergasus.R;
import io.pergasus.api.PhoenixClient;
import io.pergasus.api.PhoenixUtils;
import io.pergasus.data.Purchase;
import io.pergasus.util.maputil.RouteOverlayView.AnimType;
import io.pergasus.util.maputil.TrailSupportMapFragment;


/**
 * Track delivery of Goods in real-time
 */
public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, OnConnectionFailedListener, ConnectionCallbacks {
	
	//Constants
	public static final String EXTRA_PURCHASE = "EXTRA_PURCHASE";
	public static final int PROFILE_REQ_CODE = 122;
	public static final int RC_PLAY_SERVICES_RESOLUTION = 123;
	public static final int RC_LOCATION = 124;
	public static final long UPDATE_INTERVAL = 1000L;   //1.0 sec update interval
	public static final long FASTEST_INTERVAL = 5000L;   //1.0 sec update interval
	public static final float DISPLACEMENT = 10.0f;   //1.0 sec update interval
	
	private boolean shouldPromptForPermission;
	
	private GoogleMap map;
	private PhoenixClient client;
	private GoogleApiClient apiClient;
	private LocationRequest locationRequest;
	private Location lastLocation;
	private Purchase purchase;
	private TrailSupportMapFragment mapFragment;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracking);
		
		//Init shared preferences
		client = new PhoenixClient(this);
		
		//Setup permission primer
		shouldPromptForPermission = hasNoPermission();
		
		//Request permission once the activity starts
		if (shouldPromptForPermission) {
			if (VERSION.SDK_INT >= VERSION_CODES.M) {
				requestLocationPermission();
			}
		}
		
		//Get map fragment
		mapFragment = (TrailSupportMapFragment)
				getSupportFragmentManager().findFragmentById(R.id.map);
		//Sync map
		mapFragment.getMapAsync(this);
		
		//Get intent data
		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_PURCHASE)) {
			purchase = intent.getParcelableExtra(EXTRA_PURCHASE);
			showMessage("You are currently tracking Purchase: " + purchase.getKey());
		}
	}
	
	private boolean hasNoPermission() {
		//Checks whether user has enabled permission to access their location
		return ContextCompat.checkSelfPermission(TrackingActivity.this,
				permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
				&& ContextCompat.checkSelfPermission(TrackingActivity.this,
				permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
	}
	
	protected synchronized void buildGoogleApiClient() {
		apiClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API)
				.addApi(Places.GEO_DATA_API)
				.addApi(Places.PLACE_DETECTION_API)
				.addOnConnectionFailedListener(this)
				.addConnectionCallbacks(this)
				.build();
		apiClient.connect();
	}
	
	private void createLocationRequest() {
		locationRequest = new LocationRequest();
		locationRequest.setInterval(UPDATE_INTERVAL);
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		locationRequest.setSmallestDisplacement(DISPLACEMENT);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}
	
	@SuppressLint("MissingPermission")
	private void startLocationUpdates() {
		if (shouldPromptForPermission) return;
		if (apiClient != null && locationRequest != null) {
			LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
		} else {
			showMessage("Google API client and Location request is not properly setup");
		}
	}
	
	//Checks the availability of Google Play Services on the current device
	private boolean checkPlayService() {
		int i = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (i != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(i)) {
				GooglePlayServicesUtil.getErrorDialog(i, this, RC_PLAY_SERVICES_RESOLUTION).show();
			} else {
				showMessage("This device is not supported");
				finishAfterTransition();
			}
			return false;
		}
		
		return true;
	}
	
	private void showMessage(CharSequence message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Connect API Client
		if (apiClient != null) apiClient.connect();
	}
	
	@Override
	protected void onStop() {
		//Disconnect API client
		if (apiClient != null && apiClient.isConnected()) apiClient.disconnect();
		super.onStop();
	}
	
	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
		map.getUiSettings().setRotateGesturesEnabled(true);
		map.getUiSettings().setTiltGesturesEnabled(false);
		map.setMaxZoomPreference(18);
		
		
		//Set custom map style
		map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(),
				R.raw.zuber_map_style));
		
		//Callback for map loaded state
		map.setOnMapLoadedCallback(() -> {
			//Get user's current location
			if (lastLocation == null) {
				if (shouldPromptForPermission) {
					if (VERSION.SDK_INT >= VERSION_CODES.M) {
						requestLocationPermission();
						shouldPromptForPermission = true;
					}
				}
			} else if (checkPlayService()) {
				buildGoogleApiClient();
				createLocationRequest();
				displayLocation();
			}
		});
		
	}
	
	@SuppressLint("MissingPermission")
	private void displayLocation() {
		if (shouldPromptForPermission) {
			if (VERSION.SDK_INT >= VERSION_CODES.M) {
				requestLocationPermission();
			}
		} else {
			//Check user's login state. It may have changed before this activity is created
			if (client.isLoggedIn()) {
				if (apiClient != null) {
					if (lastLocation == null) {
						lastLocation = LocationServices.FusedLocationApi
								.getLastLocation(apiClient);
					}
					
					//Obtain user's location from last known location
					LatLng userLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
					//Get the Mall's current location
					LatLng mallGeoPoint = PhoenixUtils.INSTANCE.getMALL_GEO_POINT();
					
					//Create Bounds for location
					LatLngBounds.Builder builder = new LatLngBounds.Builder();
					//Add user's location
					builder.include(userLocation);
					//Add mall's location
					builder.include(mallGeoPoint);
					//Build bounds
					LatLngBounds bounds = builder.build();
					//Create camera updater
					CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 200);
					
					//Apply animation to map
					map.moveCamera(cameraUpdate);
					map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
					
					//Update camera movement with fragment
					map.setOnCameraMoveListener(() -> mapFragment.onCameraMove(map));
					
					//Add locations as list
					List<LatLng> routes = new ArrayList<>(0);
					routes.add(userLocation);
					routes.add(mallGeoPoint);
					
					//Finally, start animation: Can be replaces with startAnimation(routes);
					new Handler().postDelayed(() -> mapFragment.setUpPath(routes, map, AnimType.ARC), 1000);
					
				} else showMessage("Google API client cannot be created");
				
			}
		}
	}
	
	private void startAnimation(List<LatLng> routes) {
		if (map == null) {
			showMessage("Map is not ready");
		} else {
			MapAnimator.getInstance().animateRoute(map, routes);
		}
	}
	
	@TargetApi(VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == RC_LOCATION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				shouldPromptForPermission = false;
				if (checkPlayService()) {
					buildGoogleApiClient();
					createLocationRequest();
					displayLocation();
				}
				
			} else if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
				showMessage("Please accept permission to access your location");
				requestLocationPermission();
			}
		}
	}
	
	@RequiresApi(api = VERSION_CODES.M)
	private void requestLocationPermission() {
		requestPermissions(new String[]{
				permission.ACCESS_FINE_LOCATION, permission
				.ACCESS_COARSE_LOCATION}, RC_LOCATION);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//Set user's current location
		lastLocation = location;
		displayLocation();
	}
	
	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		//Show message to user
		showMessage(connectionResult.getErrorMessage());
	}
	
	@Override
	public void onConnected(@Nullable Bundle bundle) {
		//Update location and display routes
		displayLocation();
		startLocationUpdates();
	}
	
	@Override
	public void onConnectionSuspended(int i) {
		//Reconnect API client
		apiClient.connect();
	}
}
