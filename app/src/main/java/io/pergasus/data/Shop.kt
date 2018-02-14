/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

//This is the Shop model for the individual stores in the Accra Mall
class Shop : Parcelable {
    var id: Long = 0
    var name: String? = null
    var key: String? = null
    var logo: String? = null
    var motto: String? = null
    var followers_count: Long = 0
    var products_count: Long = 0
    @ServerTimestamp
    var timestamp: Date? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        key = parcel.readString()
        logo = parcel.readString()
        motto = parcel.readString()
        followers_count = parcel.readLong()
        products_count = parcel.readLong()
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor() {/*for serialization*/
    }

    constructor(id: Long = System.currentTimeMillis(), name: String?,key: String?, logo: String?,
                motto:
    String?,
                followers_count: Long = 0, products_count: Long = 0) {
        this.id = id
        this.name = name
        this.key = key
        this.logo = logo
        this.motto = motto
        this.followers_count = followers_count
        this.products_count = products_count
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(key)
        parcel.writeString(logo)
        parcel.writeString(motto)
        parcel.writeLong(followers_count)
        parcel.writeLong(products_count)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Shop> {
        override fun createFromParcel(parcel: Parcel): Shop {
            return Shop(parcel)
        }

        override fun newArray(size: Int): Array<Shop?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(existing: Shop): HashMap<String, Any?> {
        return hashMapOf(
                Pair("id", existing.id),
                Pair("name", existing.name),
                Pair("key", existing.key),
                Pair("logo", existing.logo),
                Pair("motto", existing.motto),
                Pair("followers_count", existing.followers_count),
                Pair("products_count", existing.products_count)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shop) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (key != other.key) return false
        if (logo != other.logo) return false
        if (motto != other.motto) return false
        if (followers_count != other.followers_count) return false
        if (products_count != other.products_count) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (logo?.hashCode() ?: 0)
        result = 31 * result + (motto?.hashCode() ?: 0)
        result = 31 * result + followers_count.hashCode()
        result = 31 * result + products_count.hashCode()
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }


}
