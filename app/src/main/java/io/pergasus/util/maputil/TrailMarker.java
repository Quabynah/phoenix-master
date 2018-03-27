/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.maputil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.gms.maps.model.LatLng;

public class TrailMarker {

  private LatLng mLatLng;

  private Bitmap mBitmap;

  public TrailMarker(Context context, LatLng latLng, int resource) {
    mLatLng = latLng;
    mBitmap = BitmapFactory.decodeResource(context.getResources(), resource);
  }

  public LatLng getLatLng() {
    return mLatLng;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }
}
