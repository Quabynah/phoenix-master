/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Activity
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.BuildConfig
import io.pergasus.data.PhoenixNotification

/** Abstract class for loading [PhoenixNotification] */
abstract class NotificationDataManager(private val context: Activity) :
        BaseDataManager<List<PhoenixNotification>>(context.applicationContext) {
    private var inflight: ArrayList<Query> = ArrayList(0)

    override fun cancelLoading() {
        if (inflight.size > 0) {
            inflight.clear()
        }
    }

    fun loadAllNotifications() {
        loadStarted()
        val list = ArrayList<PhoenixNotification>(0)
        val query: Query = phoenixClient.db.collection(PhoenixUtils.NOTIFICATIONS_REF)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
        inflight.add(query)
        query.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                logResult(p1.localizedMessage)
                return@EventListener
            }
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        if (doc.type == DocumentChange.Type.ADDED) {
                            val product = doc.document.toObject(PhoenixNotification::class.java)
                            list.add(product)
                        }
                    } else {
                        loadFinished()
                        onDataLoaded(list)
                    }
                }
                loadFinished()
                onDataLoaded(list)
            }
        })
    }

    private fun logResult(exception: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(NotificationDataManager::class.java.canonicalName, "Exception: $exception")
        }
    }
}