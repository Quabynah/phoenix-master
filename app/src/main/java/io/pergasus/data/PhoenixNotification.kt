/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

/** Notifications model for The Phoenix */
class PhoenixNotification : Parcelable {
    var title: String? = null
    var message: String? = null
    var from: String? = null
    var to: String? = null
    var image: String? = null
    @ServerTimestamp
    var timestamp: Date? = null

    constructor(parcel: Parcel) : this() {
        title = parcel.readString()
        message = parcel.readString()
        from = parcel.readString()
        to = parcel.readString()
        image = parcel.readString()
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }


    constructor()

    constructor(title: String, message: String, from: String, to: String, image: String) {
        this.title = title
        this.message = message
        this.from = from
        this.to = to
        this.image = image
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(message)
        parcel.writeString(from)
        parcel.writeString(to)
        parcel.writeString(image)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PhoenixNotification> {
        override fun createFromParcel(parcel: Parcel): PhoenixNotification {
            return PhoenixNotification(parcel)
        }

        override fun newArray(size: Int): Array<PhoenixNotification?> {
            return arrayOfNulls(size)
        }
    }

    /** Creates a hashMap from an existing [PhoenixNotification] */
    fun toHashMap(existing: PhoenixNotification): HashMap<String, Any?> {
        return hashMapOf(
                Pair("title", existing.title),
                Pair("message", existing.message),
                Pair("from", existing.from),
                Pair("to", existing.to),
                Pair("image", existing.image),
                Pair("timestamp", Date(System.currentTimeMillis()))
        )
    }

    override fun toString(): String {
        return "PhoenixNotification(message=$message, from=$from, to=$to, timestamp=$timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhoenixNotification) return false

        if (title != other.title) return false
        if (message != other.message) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (image != other.image) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }


}
