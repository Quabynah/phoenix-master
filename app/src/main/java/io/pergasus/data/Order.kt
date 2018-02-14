/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

class Order : Parcelable {
    var id: Long = 0
    var name: String? = null
    var image: String? = null
    var price: String? = null
    var quantity: String? = null
    @ServerTimestamp
    var timestamp: Date? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        image = parcel.readString()
        price = parcel.readString()
        quantity = parcel.readString()
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor()

    constructor(id: Long = System.currentTimeMillis(), name: String?, image: String?, price: String?, quantity: String?) {
        this.id = id
        this.name = name
        this.image = image
        this.price = price
        this.quantity = quantity
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(image)
        parcel.writeString(price)
        parcel.writeString(quantity)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Order(id=$id, name=$name, image=$image, price=$price, quantity=$quantity, timestamp=$timestamp)"
    }

    companion object CREATOR : Parcelable.Creator<Order> {
        override fun createFromParcel(parcel: Parcel): Order {
            return Order(parcel)
        }

        override fun newArray(size: Int): Array<Order?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(existing: Order): HashMap<String, Any?> {
        return hashMapOf(
                Pair("id", existing.id),
                Pair("name", existing.name),
                Pair("image", existing.image),
                Pair("price", existing.price),
                Pair("quantity", existing.quantity),
                Pair("timestamp", Date(System.currentTimeMillis()))
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Order) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (image != other.image) return false
        if (price != other.price) return false
        if (quantity != other.quantity) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + (quantity?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }
}