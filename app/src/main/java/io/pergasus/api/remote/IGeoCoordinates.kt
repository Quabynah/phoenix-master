/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Project : phoenix-master
 * Created by Dennis Bilson on Wed at 9:27 PM.
 * Package name : io.pergasus.api.remote
 */

interface IGeoCoordinates {
    //Get GeoCode API
    @GET("maps/api/geocode/json")
    fun getGeoCode(@Query("address") address: String): Call<String>

    //Get Directions API
    @GET("maps/api/directions/json")
    fun getDirections(@Query("origin") origin: String, @Query("destination") destination: String): Call<String>

}
