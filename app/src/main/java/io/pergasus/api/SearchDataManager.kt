/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.BuildConfig
import io.pergasus.data.Product


/**
 * Responsible for loading search results from all categories. Instantiating classes are
 * responsible for providing the [onDataLoaded] method to do something with the data.
 */
abstract class SearchDataManager(private val context: Activity) : BaseDataManager<List<ProductItem>>(context) {
    private var inflight: ArrayList<Task<QuerySnapshot>> = ArrayList(0)
    // state
    private var query = ""
    private var page = 1

    override fun cancelLoading() {
        if (inflight.size > 0) {
            inflight.clear()
        }
    }

    fun searchFor(query: String) {
        if (this.query != query) {
            clear()
            this.query = query
        } else {
            page++
        }
        searchAllProducts(query)
    }

    private fun searchAllProducts(query: String) {
        loadStarted()
        val products: ArrayList<Product> = ArrayList(0)
        //Query Database
        val dbQuery: Query = phoenixClient.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.ALL_CATEGORY_REF)
                .orderBy("timestamp")
//                .startAt(query)
//                .endAt(query + "\uf8ff")
                .limit(100)
        //Listen for changes in the database reference
        dbQuery.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                logResult(p1.localizedMessage)
                return@EventListener
            }
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (product.name?.contains(query, true)!!
                                || product.tag?.contains(query, true)!!
                                || product.category?.contains(query, true)!!
                                || product.description?.contains(query, true)!!) {
                            products.add(product)
                        }
                    } else {
                        loadFinished()
                        setDataSource(products, Source.PhoenixSearchSource.QUERY_PREFIX + query)
                        onDataLoaded(products)
                    }
                }
                loadFinished()
                setDataSource(products, Source.PhoenixSearchSource.QUERY_PREFIX + query)
                onDataLoaded(products)
            }

        })

    }

    fun loadMore() {
        searchFor(query)
    }

    fun clear() {
        cancelLoading()
        query = ""
        page = 1
        resetLoadingCount()
    }

    fun getQuery(): String? {
        return query
    }

    private fun logResult(exception: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(SearchDataManager::class.java.canonicalName, "Exception: $exception")
        }
    }

}
