/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import io.pergasus.R.string.address
import java.util.*

class Customer : Parcelable {

    var id: Long = 0
    var name: String? = null
    var photo: String? = null
    var info: String? = null
    var key: String? = null
    var tokenId: String? = null
    var addressLat: String? = null
    var addressLng: String? = null

    @ServerTimestamp
    var timestamp: Date? = null

    constructor()


    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        photo = parcel.readString()
        info = parcel.readString()
        key = parcel.readString()
        addressLat = parcel.readString()
        addressLng = parcel.readString()
        tokenId = parcel.readString()
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
    }

    constructor(id: Long = System.currentTimeMillis(), name: String?, photo: String?, info: String?, key: String?,
                tokenId: String?, addressLat: String?, addressLng: String?, timestamp: Date) {
        this.id = id
        this.name = name
        this.photo = photo
        this.info = info
        this.key = key
        this.tokenId = tokenId
        this.addressLat = addressLat
        this.addressLng = addressLng
        this.timestamp = timestamp
    }

    /** Builder for customer model */
    class Builder {
        private var id: Long = 0
        private var name: String? = null
        private var info: String? = null
        private var key: String? = null
        private var photo: String? = null
        private var addressLat: String? = null
        private var addressLng: String? = null
        private var tokenId: String? = null

        fun setId(id: Long): Builder {
            this.id = id
            return this
        }

        fun setName(customerName: String): Builder {
            this.name = customerName
            return this
        }

        fun setInfo(customerInfo: String): Builder {
            this.info = customerInfo
            return this
        }

        fun setKey(customerID: String): Builder {
            this.key = customerID
            return this
        }

        fun setPhoto(customerPhoto: String): Builder {
            this.photo = customerPhoto
            return this
        }

        fun setAddressLat(addressLat: String): Builder {
            this.addressLat = addressLat
            return this
        }

        fun setAddressLng(addressLng: String): Builder {
            this.addressLng = addressLng
            return this
        }

        fun setTokenId(tokenId: String): Builder {
            this.tokenId = tokenId
            return this
        }

        fun build(): Customer {
            return Customer(id, name!!, photo!!, info!!, key!!, tokenId, addressLat, addressLng, Date(System.currentTimeMillis()))
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(photo)
        parcel.writeString(info)
        parcel.writeString(key)
        parcel.writeString(addressLat)
        parcel.writeString(addressLng)
        parcel.writeString(tokenId)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
    }

    override fun describeContents(): Int {
        return 0
    }


    companion object CREATOR : Parcelable.Creator<Customer> {
        override fun createFromParcel(parcel: Parcel): Customer {
            return Customer(parcel)
        }

        override fun newArray(size: Int): Array<Customer?> {
            return arrayOfNulls(size)
        }
    }

    /**
     * Creates a HashMap of the customer data model
     */
    fun toHashMap(customer: Customer): HashMap<String, Any?> {
        return hashMapOf(
                Pair<String, Any?>("id", customer.id),
                Pair<String, Any?>("name", customer.name),
                Pair<String, Any?>("photo", customer.photo),
                Pair<String, Any?>("info", customer.info),
                Pair<String, Any?>("key", customer.key),
                Pair<String, Any?>("addressLat", customer.addressLat),
                Pair<String, Any?>("addressLng", customer.addressLng),
                Pair<String, Any?>("tokenId", customer.tokenId),
                Pair<String, Any?>("timestamp", customer.timestamp)
        )
    }

    override fun toString(): String {
        return "Customer(id=$id, name=$name, photo=$photo, info=$info, key=$key, tokenId=$tokenId, address=$address, timestamp=$timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Customer) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (photo != other.photo) return false
        if (info != other.info) return false
        if (key != other.key) return false
        if (tokenId != other.tokenId) return false
        if (addressLat != other.addressLat) return false
        if (addressLng != other.addressLng) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (photo?.hashCode() ?: 0)
        result = 31 * result + (info?.hashCode() ?: 0)
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (tokenId?.hashCode() ?: 0)
        result = 31 * result + (addressLat?.hashCode() ?: 0)
        result = 31 * result + (addressLng?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }


}