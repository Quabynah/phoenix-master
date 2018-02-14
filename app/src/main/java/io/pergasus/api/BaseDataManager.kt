/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.content.Context
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for loading data; extending types are responsible for providing implementations of
 * [.onDataLoaded] to do something with the data and [.cancelLoading] to
 * cancel any activity.
 */
abstract class BaseDataManager<in T>(context: Context) : DataLoadingSubject {

    private val loadingCount: AtomicInteger = AtomicInteger(0)
    val phoenixClient: PhoenixClient = PhoenixClient(context)
    private var loadingCallbacks: MutableList<DataLoadingSubject.DataLoadingCallbacks>? = null

    override val isDataLoading: Boolean
        get() = loadingCount.get() > 0

    abstract fun onDataLoaded(data: T)

    abstract fun cancelLoading()

    override fun registerCallback(callbacks: DataLoadingSubject.DataLoadingCallbacks) {
        if (loadingCallbacks == null) {
            loadingCallbacks = ArrayList(1)
        }
        loadingCallbacks!!.add(callbacks)
    }

    override fun unregisterCallback(callbacks: DataLoadingSubject.DataLoadingCallbacks) {
        if (loadingCallbacks != null && loadingCallbacks!!.contains(callbacks)) {
            loadingCallbacks!!.remove(callbacks)
        }
    }

    protected fun loadStarted() {
        if (0 == loadingCount.getAndIncrement()) {
            dispatchLoadingStartedCallbacks()
        }
    }

    protected fun loadFinished() {
        if (0 == loadingCount.decrementAndGet()) {
            dispatchLoadingFinishedCallbacks()
        }
    }

    protected fun resetLoadingCount() {
        loadingCount.set(0)
    }

    protected fun dispatchLoadingStartedCallbacks() {
        if (loadingCallbacks == null || loadingCallbacks!!.isEmpty()) return
        for (loadingCallback in loadingCallbacks!!) {
            loadingCallback.dataStartedLoading()
        }
    }

    protected fun dispatchLoadingFinishedCallbacks() {
        if (loadingCallbacks == null || loadingCallbacks!!.isEmpty()) return
        for (loadingCallback in loadingCallbacks!!) {
            loadingCallback.dataFinishedLoading()
        }
    }

    companion object {

        @JvmStatic
        protected fun setDataSource(items: List<ProductItem>, dataSource: String) {
            for (item in items) {
                item.dataSource = dataSource
            }
        }
    }

}
