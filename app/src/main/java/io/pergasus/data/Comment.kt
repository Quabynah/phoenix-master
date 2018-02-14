/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import android.text.Spanned
import android.text.TextUtils
import android.widget.TextView
import com.google.firebase.firestore.ServerTimestamp
import io.pergasus.util.PhoenixUtils
import java.util.*

class Comment : Parcelable {

    var id: Long = 0
    var body: String? = null
    var likes_count: Long = 0
    var user: Customer? = null
    var likes_url: String? = null
    @ServerTimestamp
    var timestamp: Date? = null
    @ServerTimestamp
    var updated_at: Date? = null

    var liked: Boolean? = null
    var parsedBody: Spanned? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        body = parcel.readString()
        likes_count = parcel.readLong()
        user = parcel.readParcelable(Customer::class.java.classLoader)
        likes_url = parcel.readString()
        liked = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        val tmp1 = parcel.readLong()
        timestamp = if (tmp1 > -1L) Date(tmp1) else null
        val tmp2 = parcel.readLong()
        updated_at = if (tmp2 > -1L) Date(tmp2) else null
    }

    constructor()

    constructor(id: Long = System.currentTimeMillis(),
                body: String,
                likes_count: Long,
                likes_url: String,
                user: Customer, timestamp: Date) {
        this.id = id
        this.body = body
        this.likes_count = likes_count
        this.likes_url = likes_url
        this.user = user
        this.timestamp = timestamp
    }


    fun getParsedBody(textView: TextView): Spanned? {
        if (parsedBody == null && !TextUtils.isEmpty(body)) {
            parsedBody = PhoenixUtils.parsePhoenixHtml(body.toString(), textView.linkTextColors,
                    textView.highlightColor)
        }
        return parsedBody
    }

    override fun toString(): String {
        return body.toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(body)
        parcel.writeLong(likes_count)
        parcel.writeParcelable(user, flags)
        parcel.writeString(likes_url)
        parcel.writeValue(liked)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
        parcel.writeLong(if (updated_at != null) updated_at!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Comment> {
        override fun createFromParcel(parcel: Parcel): Comment {
            return Comment(parcel)
        }

        override fun newArray(size: Int): Array<Comment?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(existing: Comment): HashMap<String, Any?> {
        return hashMapOf(
                Pair<String, Any?>("id", existing.id),
                Pair<String, Any?>("body", existing.body),
                Pair<String, Any?>("likes_count", existing.likes_count),
                Pair<String, Any?>("user", existing.user?.toHashMap(existing.user!!)),
                Pair<String, Any?>("likes_url", existing.likes_url)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Comment) return false

        if (id != other.id) return false
        if (body != other.body) return false
        if (likes_count != other.likes_count) return false
        if (user != other.user) return false
        if (likes_url != other.likes_url) return false
        if (timestamp != other.timestamp) return false
        if (updated_at != other.updated_at) return false
        if (liked != other.liked) return false
        if (parsedBody != other.parsedBody) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + likes_count.hashCode()
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + (likes_url?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        result = 31 * result + (updated_at?.hashCode() ?: 0)
        result = 31 * result + (liked?.hashCode() ?: 0)
        result = 31 * result + (parsedBody?.hashCode() ?: 0)
        return result
    }


}