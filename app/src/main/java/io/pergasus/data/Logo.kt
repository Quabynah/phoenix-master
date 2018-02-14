/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data

/** Logo model for the application */
class Logo {
    var url: String? = null

    constructor()

    constructor(url: String) {
        this.url = url
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Logo) return false

        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        return url?.hashCode() ?: 0
    }

}
