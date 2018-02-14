/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.data.Follow
import io.pergasus.data.Shop


abstract class FollowersDataManager(context: Context, private val shop: Shop) :
        PaginatedDataManager<List<Follow>>(context) {

    private var inflight: ArrayList<Task<QuerySnapshot>> = ArrayList(0)

    override fun cancelLoading() {
        if (inflight.size > 0) {
            inflight.clear()
        }
    }

    override fun loadData(page: Int) {
        val get = phoenixClient.db.document(PhoenixUtils.FOLLOW_REF)
                .collection(shop.key!!)
                .get()
        inflight.add(get)

        get.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val list = ArrayList<Follow>(0)
                for (item in task.result.documentChanges) {
                    if (item.document.exists()) {
                        val product = item.document.toObject(Follow::class.java)
                        list.add(product)
                        onDataLoaded(list)
                        loadFinished()
                        moreDataAvailable = list.isNotEmpty()
                        inflight.remove(get)
                    } else {
                        onDataLoaded(list)
                        loadFinished()
                        moreDataAvailable = false
                    }
                }
            } else {
                logResult(task.exception?.localizedMessage)
            }
        }.addOnFailureListener { exception ->
            logResult(exception.localizedMessage)
        }
    }

    @SuppressLint("LogConditional")
    private fun logResult(exception: String?) {
        loadFinished()
        moreDataAvailable = false
        Log.d(FollowersDataManager::class.java.canonicalName, "Exception: $exception")
    }
}
