/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import io.pergasus.data.ProductId

/**
 * Base class for all model types
 */
abstract class ProductItem : ProductId() {
    open var id: Long = 0
    open val name: String? = null
    open var url: String? = null // can't be final as some APIs use different serialized names
    open var dataSource: String? = null
    open var page: Int = 0
    open var weight: Float = 0.toFloat() // used for sorting
    open var colspan: Int = 0

    override fun toString(): String {
        return name!!
    }

    /**
     * Equals check based on the id field
     */
    override fun equals(other: Any?): Boolean {
        return other!!.javaClass == javaClass && (other as ProductItem).id == id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (dataSource?.hashCode() ?: 0)
        result = 31 * result + page
        result = 31 * result + weight.hashCode()
        result = 31 * result + colspan
        return result
    }


}
