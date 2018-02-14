/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import io.pergasus.api.FollowerListable
import java.util.*

class Follow : Parcelable, FollowerListable {
    override var id: Long = 0
    var shop: Shop? = null
    override var customer: Customer? = null
    @ServerTimestamp
    var timestamp: Date? = null

    override val dateCreated: Date?
        get() = timestamp

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        shop = parcel.readParcelable(Shop::class.java.classLoader)
        customer = parcel.readParcelable(Customer::class.java.classLoader)
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor() {/*Serialization*/
    }

    constructor(id: Long = System.currentTimeMillis(), shop: Shop? = null, customer: Customer? =
    null, timestamp: Date) {
        this.id = id
        this.shop = shop
        this.customer = customer
        this.timestamp = timestamp
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeParcelable(shop, flags)
        parcel.writeParcelable(customer, flags)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Follow(id=$id, shop=$shop, customer=$customer, timestamp=$timestamp)"
    }

    companion object CREATOR : Parcelable.Creator<Follow> {
        override fun createFromParcel(parcel: Parcel): Follow {
            return Follow(parcel)
        }

        override fun newArray(size: Int): Array<Follow?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(existing: Follow): HashMap<String, Any?> {
        return hashMapOf(
                Pair("id", existing.id),
                Pair("shop", existing.shop?.toHashMap(existing.shop!!)),
                Pair("customer", existing.customer?.toHashMap(existing.customer!!))
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Follow) return false

        if (id != other.id) return false
        if (shop != other.shop) return false
        if (customer != other.customer) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (shop?.hashCode() ?: 0)
        result = 31 * result + (customer?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }

}