/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util;


import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for working with Parcels.
 */
public class ParcelUtils {
	
	private ParcelUtils() {
	}
	
	public static void writeStringMap(Map<String, String> map, Parcel parcel) {
		if (map != null && !map.isEmpty()) {
			parcel.writeInt(map.size());
			for (Map.Entry<String, String> entry : map.entrySet()) {
				parcel.writeString(entry.getKey());
				parcel.writeString(entry.getValue());
			}
		} else {
			parcel.writeInt(0);
		}
	}
	
	public static Map<String, String> readStringMap(Parcel parcel) {
		Map<String, String> map = null;
		int size = parcel.readInt();
		if (size > 0) {
			map = new HashMap<String, String>(size);
			for (int i = 0; i < size; i++) {
				String key = parcel.readString();
				String value = parcel.readString();
				map.put(key, value);
			}
		}
		return map;
	}
}
