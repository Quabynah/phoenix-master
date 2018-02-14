/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

class Like : Parcelable {
    var id: Long = 0
    var customer: Customer? = null
    var comment: Comment? = null
    @ServerTimestamp
    var timestamp: Date? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        customer = parcel.readParcelable(Customer::class.java.classLoader)
        comment = parcel.readParcelable(Comment::class.java.classLoader)
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor()

    constructor(id: Long = System.currentTimeMillis(), customer: Customer, comment: Comment) {
        this.id = id
        this.customer = customer
        this.comment = comment
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeParcelable(customer, flags)
        parcel.writeParcelable(comment, flags)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Like(id=$id, customer=$customer, comment=$comment, timestamp=$timestamp)"
    }

    companion object CREATOR : Parcelable.Creator<Like> {
        override fun createFromParcel(parcel: Parcel): Like {
            return Like(parcel)
        }

        override fun newArray(size: Int): Array<Like?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(like: Like): HashMap<String,Any?>{
        return hashMapOf(
                Pair<String,Any?>("id",like.id),
                Pair<String,Any?>("customer",customer?.toHashMap(customer!!)),
                Pair<String,Any?>("comment",comment?.toHashMap(comment!!))
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Like) return false

        if (id != other.id) return false
        if (customer != other.customer) return false
        if (comment != other.comment) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (customer?.hashCode() ?: 0)
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }

}
