/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.annotation.SuppressLint
import android.app.Activity
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.data.Purchase
import timber.log.Timber

/** Model class for loading live orders */
abstract class OrderDataManager(private val context: Activity) :
        BaseDataManager<List<Purchase>>(context.applicationContext) {
    private var inflight: ArrayList<Query> = ArrayList(0)

    override fun cancelLoading() {
        if (inflight.size > 0) {
            inflight.clear()
        }
    }

    /** Loads data from the user's orders database reference */
    fun loadData(userKey: String) {
        loadStarted()
        val query = phoenixClient.db.document(PhoenixUtils.PURCHASE_REF)
                .collection(userKey)
                .orderBy("timestamp")
                .limit(60)
        inflight.add(query)
        query.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                logResult(p1.localizedMessage, query)
                return@EventListener
            }

            if (p0 != null) {
                val list = ArrayList<Purchase>(0)
                for (item in p0.documents) {
                    if (item.exists()) {
                        val docRef = item.id
                        val order = item.toObject(Purchase::class.java).withId<Purchase>(docRef)
                        list.add(order)
                    }
                }
                loadFinished()
                onDataLoaded(list)
                inflight.remove(query)
            }
        })
    }

    @SuppressLint("LogConditional")
    private fun logResult(exception: String?, listener: Query) {
        loadFinished()
        inflight.remove(listener)
        Timber.d("Exception: $exception")
    }

}