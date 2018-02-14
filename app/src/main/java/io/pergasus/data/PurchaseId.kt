/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.support.annotation.NonNull

/** Returns a [com.google.firebase.firestore.FirebaseFirestore] document id for the subclass */
@Suppress("UNCHECKED_CAST")
open class PurchaseId {
    open var purchaseId: String? = null


    open fun <T : PurchaseId> withId(@NonNull id: String): T {
        this.purchaseId = id
        return this as T
    }
}