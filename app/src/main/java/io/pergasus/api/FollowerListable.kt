/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import io.pergasus.data.Customer
import java.util.*

/**
 * Create followers list
 */
interface FollowerListable {
    val customer: Customer?
    val id: Long?
    val dateCreated: Date?
}
