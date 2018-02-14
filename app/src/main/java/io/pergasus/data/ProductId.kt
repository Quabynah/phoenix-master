/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.support.annotation.NonNull

/** For retrieving [com.google.firebase.firestore.FirebaseFirestore] */
@Suppress("UNCHECKED_CAST")
open class ProductId {
    open var productId: String? = null

    open fun <T : ProductId> withId(@NonNull id: String): T {
        this.productId = id
        return this as T
    }
}
