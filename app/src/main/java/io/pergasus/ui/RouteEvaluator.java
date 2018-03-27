/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui;

import android.animation.TypeEvaluator;

import com.google.android.gms.maps.model.LatLng;

public class RouteEvaluator implements TypeEvaluator<LatLng> {
	@Override
	public LatLng evaluate(float t, LatLng startPoint, LatLng endPoint) {
		double lat = startPoint.latitude + t * (endPoint.latitude - startPoint.latitude);
		double lng = startPoint.longitude + t * (endPoint.longitude - startPoint.longitude);
		return new LatLng(lat, lng);
	}
}
