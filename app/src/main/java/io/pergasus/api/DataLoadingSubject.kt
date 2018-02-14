/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

/**
 * Project : e-commerce-android
 * Created by Dennis Bilson on Thu at 7:20 AM.
 * Package name : io.app.api
 */

//An interface for classes offering data loading state to be observed.
interface DataLoadingSubject {
    val isDataLoading: Boolean
    fun registerCallback(callbacks: DataLoadingCallbacks)
    fun unregisterCallback(callbacks: DataLoadingCallbacks)

    interface DataLoadingCallbacks {
        fun dataStartedLoading()
        fun dataFinishedLoading()
    }
}
