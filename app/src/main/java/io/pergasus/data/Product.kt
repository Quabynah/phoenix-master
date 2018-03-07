/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.ColorInt
import android.text.Spanned
import android.text.TextUtils
import com.google.firebase.firestore.ServerTimestamp
import io.pergasus.api.ProductItem
import io.pergasus.util.PhoenixUtils
import java.util.*

/** [Product] */
class Product : ProductItem, Parcelable {

    override var id: Long = 0

    override var name: String? = null

    var description: String? = null

    var category: String? = null

    override var url: String? = null

    var price: String? = null

    var discount: String? = null

    var tag: String? = null

    var quantity: String? = null

    var brand: List<String>? = null

    var animated = false

    var shop: String? = null

    var logo: String? = null

    var key: String? = null

    var shopID: String? = null

    @ServerTimestamp
    var timestamp: Date? = null

    var hasFadedIn = false
    var parsedDescription: Spanned? = null

    constructor()

    constructor(id: Long = System.currentTimeMillis(), name: String, description: String, category: String, url: String, price: String,
                discount: String, tag: String, quantity: String, brand: List<String>, animated: Boolean,
                shop: String, logo: String, key: String? = null, shopID: String = "") : super() {
        this.id = id
        this.name = name
        this.description = description
        this.category = category
        this.url = url
        this.price = price
        this.discount = discount
        this.tag = tag
        this.quantity = quantity
        this.brand = brand
        this.animated = animated
        this.shop = shop
        this.logo = logo
        this.key = key
        this.shopID = shopID
    }

    fun getParsedDescription(linkTextColor: ColorStateList,
                             @ColorInt linkHighlightColor: Int): Spanned? {
        if (parsedDescription == null && !TextUtils.isEmpty(description)) {
            parsedDescription = PhoenixUtils.parsePhoenixHtml(description!!, linkTextColor,
                    linkHighlightColor)
        }
        return parsedDescription
    }

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        description = parcel.readString()
        category = parcel.readString()
        url = parcel.readString()
        price = parcel.readString()
        discount = parcel.readString()
        tag = parcel.readString()
        quantity = parcel.readString()
        brand = parcel.createStringArrayList()
        animated = parcel.readByte() != 0.toByte()
        shop = parcel.readString()
        logo = parcel.readString()
        key = parcel.readString()
        shopID = parcel.readString()
        val tmp = parcel.readLong()
        timestamp = if (tmp > -1L) Date(tmp) else null
        hasFadedIn = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeString(url)
        parcel.writeString(price)
        parcel.writeString(discount)
        parcel.writeString(tag)
        parcel.writeString(quantity)
        parcel.writeStringList(brand)
        parcel.writeByte(if (animated) 1 else 0)
        parcel.writeString(shop)
        parcel.writeString(logo)
        parcel.writeString(key)
        parcel.writeString(shopID)
        parcel.writeLong(if (timestamp != null) timestamp!!.time else -1L)
        parcel.writeByte(if (hasFadedIn) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }


    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }

    fun toHashMap(existing: Product): HashMap<String, Any?> {
        return hashMapOf(
                Pair("id", existing.id),
                Pair("name", existing.name),
                Pair("description", existing.description),
                Pair("category", existing.category),
                Pair("url", existing.url),
                Pair("price", existing.price),
                Pair("discount", existing.discount),
                Pair("tag", existing.tag),
                Pair("quantity", existing.quantity),
                Pair("brand", existing.brand),
                Pair("animated", existing.animated),
                Pair("shop", existing.shop),
                Pair("logo", existing.logo),
                Pair("key", existing.key),
                Pair("shopID", existing.shopID)
        )
    }




    override fun toString(): String {
        return "Product(id=$id, name=$name, description=$description, category=$category, url=$url, price=$price, discount=$discount, tag=$tag, quantity=$quantity, brand=$brand, animated=$animated, shop=$shop, logo=$logo, key=$key, shopID=$shopID, timestamp=$timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Product) return false
        if (!super.equals(other)) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (category != other.category) return false
        if (url != other.url) return false
        if (price != other.price) return false
        if (discount != other.discount) return false
        if (tag != other.tag) return false
        if (quantity != other.quantity) return false
        if (brand != other.brand) return false
        if (animated != other.animated) return false
        if (shop != other.shop) return false
        if (logo != other.logo) return false
        if (key != other.key) return false
        if (shopID != other.shopID) return false
        if (timestamp != other.timestamp) return false
        if (hasFadedIn != other.hasFadedIn) return false
        if (parsedDescription != other.parsedDescription) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + (discount?.hashCode() ?: 0)
        result = 31 * result + (tag?.hashCode() ?: 0)
        result = 31 * result + (quantity?.hashCode() ?: 0)
        result = 31 * result + (brand?.hashCode() ?: 0)
        result = 31 * result + animated.hashCode()
        result = 31 * result + (shop?.hashCode() ?: 0)
        result = 31 * result + (logo?.hashCode() ?: 0)
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (shopID?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        result = 31 * result + hasFadedIn.hashCode()
        result = 31 * result + (parsedDescription?.hashCode() ?: 0)
        return result
    }


}
