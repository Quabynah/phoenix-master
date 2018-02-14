/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.content.Context

/**
 * Load a paginated data source. Instantiating classes are responsible for providing implementations
 * of [.loadData] to actually load the data, and [.onDataLoaded] to do something
 * with it.
 */

abstract class PaginatedDataManager<in T>(context: Context) : BaseDataManager<T>(context) {
    // state
    private var page = 0
    protected var moreDataAvailable = true

    fun loadData() {
        if (!moreDataAvailable) return
        page++
        loadStarted()
        loadData(page)
    }

    /**
     * Extending classes must provide this method to actually load data. They must call
     * [loadFinished] when finished.
     */
    protected abstract fun loadData(page: Int)

}
