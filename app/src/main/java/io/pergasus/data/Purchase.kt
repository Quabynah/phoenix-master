/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.ArrayList


class Purchase : Parcelable, PurchaseId {
    var id: Long = 0
        private set
    var key: String? = null
    var price: String? = null
    var items: List<Order> = ArrayList(0)

    @ServerTimestamp
    var timestamp: Date? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        key = parcel.readString()
        price = parcel.readString()
        items = parcel.createTypedArrayList(Order)
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor()

    constructor(id: Long = System.currentTimeMillis(), key: String, price: String, items: List<Order>) {
        this.id = id
        this.key = key
        this.price = price
        this.items = items
        this.timestamp = Date(System.currentTimeMillis())
    }

    class Builder {
        private var id: Long = 0
        private var key: String? = null
        private var price: String? = null
        private var items: ArrayList<Order> = ArrayList(0)

        fun setId(id: Long): Builder {
            this.id = id
            return this
        }

        fun setKey(key: String): Builder {
            this.key = key
            return this
        }

        fun setPrice(price: String): Builder {
            this.price = price
            return this
        }

        fun setOrderItems(vararg orders: Order): Builder {
            if (orders.isNotEmpty()) {
                for (order in orders) {
                    this.items.add(order)
                }
            }
            return this
        }

        fun build(): Purchase {
            return Purchase(id, key!!, price!!, items)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(key)
        parcel.writeString(price)
        parcel.writeTypedList(items)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Purchase) return false

        if (id != other.id) return false
        if (key != other.key) return false
        if (price != other.price) return false
        if (items != other.items) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + items.hashCode()
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }


    companion object CREATOR : Parcelable.Creator<Purchase> {
        override fun createFromParcel(parcel: Parcel): Purchase {
            return Purchase(parcel)
        }

        override fun newArray(size: Int): Array<Purchase?> {
            return arrayOfNulls(size)
        }
    }


}
