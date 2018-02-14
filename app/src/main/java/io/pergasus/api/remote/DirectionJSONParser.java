/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api.remote;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.pergasus.BuildConfig;

/**
 * Decodes polyLines on the internet
 */
public class DirectionJSONParser {
	private static final String TAG = "DirectionJSONParser";
	
	/**
	 * Receives a JSONObject and returns a list of lists containing latitude and longitude
	 */
	public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
		
		List<List<HashMap<String, String>>> routes = new ArrayList<>(0);
		
		try {
			
			JSONArray jRoutes = jObject.getJSONArray("routes");
			
			/* Traversing all routes */
			for (int i = 0; i < jRoutes.length(); i++) {
				JSONArray jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
				List path = new ArrayList<HashMap<String, String>>(0);
				
				/* Traversing all legs */
				for (int j = 0; j < jLegs.length(); j++) {
					JSONArray jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
					
					/* Traversing all steps */
					for (int k = 0; k < jSteps.length(); k++) {
						String polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
						List list = decodePoly(polyline);
						
						/* Traversing all points */
						for (int m = 0; m < list.size(); m++) {
							HashMap<String, String> hm = new HashMap<>(0);
							hm.put("lat", Double.toString(((LatLng) list.get(m)).latitude));
							hm.put("lng", Double.toString(((LatLng) list.get(m)).longitude));
							path.add(hm);
						}
					}
					routes.add(path);
				}
			}
			
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(TAG, "parse: " + e.getLocalizedMessage());
		} catch (RuntimeException ignored) {
		}
		
		return routes;
	}
	
	/**
	 * Method to decode polyline points
	 * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
	 */
	private List decodePoly(String encoded) {
		List poly = new ArrayList();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;
		
		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			
			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;
			
			LatLng p = new LatLng((((double) lat / 1E5)),
					(((double) lng / 1E5)));
			poly.add(p);
		}
		
		return poly;
	}
}
