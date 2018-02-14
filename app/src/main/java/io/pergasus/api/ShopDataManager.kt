/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import io.pergasus.data.Product
import io.pergasus.data.Shop

/**
 * Responsible for loading a shop's products. Instantiating classes are
 * responsible for providing the [onDataLoaded] method to do something with the data.
 */
abstract class ShopDataManager(context: Context, private val shop: Shop) :
        PaginatedDataManager<List<Product>>(context) {
    private var inflight: ArrayList<Query> = ArrayList(0)

    override fun cancelLoading() {
        if (inflight.size > 0) {
            inflight.clear()
        }
    }

    override fun loadData(page: Int) {
        loadStarted()
        loadShopData()
    }

    private fun loadShopData() {
        val listener = phoenixClient.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.ALL_CATEGORY_REF)
                .whereEqualTo("shop", shop.name)
                .orderBy("name")
                .limit(50)
        inflight.add(listener)
        listener.addSnapshotListener { p0, p1 ->
            if (p1 != null) {
                logResult(p1.localizedMessage, listener)
                return@addSnapshotListener
            }
            if (p0 != null) {
                val list = ArrayList<Product>(0)
                for (item in p0.documentChanges) {
                    if (item.document.exists()) {
                        if (item.type == DocumentChange.Type.ADDED) {
                            val docId = item.document.id
                            val product = item.document.toObject(Product::class.java).withId<Product>(docId)
                            list.add(product)
                            moreDataAvailable = list.isNotEmpty()
                        }
                    } else {
                        loadFinished()
                        moreDataAvailable = false
                    }
                }
                sourceLoaded(list, listener)
            }
        }
    }

    /*moreDataAvailable = if (item.exists()) {
                        val product = item.toObject(Product::class.java)
                        list.add(product)
                        list.isNotEmpty()
                    } else {
                        loadFinished()
                        false
                    }*/

    private fun sourceLoaded(list: ArrayList<Product>, listener: Query) {
        setDataSource(list, SOURCE_SHOP_DATA)
        onDataLoaded(list)
        loadFinished()
        inflight.remove(listener)
    }

    @SuppressLint("LogConditional")
    private fun logResult(exception: String?, listener: Query) {
        loadFinished()
        moreDataAvailable = false
        inflight.remove(listener)
        Log.d(ShopDataManager::class.java.canonicalName, "Exception: $exception")
    }

    companion object {
        private val SOURCE_SHOP_DATA = "SOURCE_SHOP_DATA"
    }
}
