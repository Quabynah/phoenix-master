/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.recyclerview

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import io.pergasus.api.DataLoadingSubject

/**
 * A scroll listener for RecyclerView to load more items as you approach the end.
 *
 * Adapted from https://gist.github.com/ssinss/e06f12ef66c51252563e
 */
abstract class InfiniteScrollListener(private val layoutManager: LinearLayoutManager,
                                      private val dataLoading: DataLoadingSubject) :
        RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        // bail out if scrolling upward or already loading data
        if (dy < 0 || dataLoading.isDataLoading) return

        val visibleItemCount = recyclerView!!.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + VISIBLE_THRESHOLD)) {
            onLoadMore()
        }
    }

    companion object {
        // The minimum number of items remaining before we should loading more.
        private val VISIBLE_THRESHOLD = 5
    }

    abstract fun onLoadMore()

}