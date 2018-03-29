/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.maputil;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;


public class TrailSupportMapFragment extends SupportMapFragment {
  public View mOriginalContentView;
  public FrameLayout mContainerLayout;
  //public FrameLayout mRoutViewHolder;
  public RouteOverlayView mRouteOverlayView;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
    mRouteOverlayView = new RouteOverlayView(getActivity().getApplicationContext());

    mContainerLayout = new FrameLayout(getActivity().getApplicationContext());
    mContainerLayout.addView(mOriginalContentView);

    //mRoutViewHolder = new FrameLayout(getActivity().getApplicationContext());
    //mRoutViewHolder.addView(mRouteOverlayView);

    mContainerLayout.addView(mRouteOverlayView);
    return mContainerLayout;
  }

  @Override
  public View getView() {
    return mOriginalContentView;
  }

  public void onCameraMove(GoogleMap map) {
    mRouteOverlayView.onCameraMove(map);
  }

  public void setUpPath(List<LatLng> route, GoogleMap map, RouteOverlayView.AnimType animType) {
    //setUpLoadPath(route.get(0), route.get(route.size()-1), map);
    //new Handler().postDelayed(new Runnable() {
    //  @Override public void run() {
        mRouteOverlayView.setUpPath(route, map, animType);
    //  }
    //}, 5000);

  }

  private void setUpLoadPath(LatLng fromLatlng, LatLng toLatlng, GoogleMap map) {
    mRouteOverlayView.loadPath(fromLatlng, toLatlng, map);
  }

  public RouteOverlayView getOverlayView() {
    return mRouteOverlayView;
  }
}
