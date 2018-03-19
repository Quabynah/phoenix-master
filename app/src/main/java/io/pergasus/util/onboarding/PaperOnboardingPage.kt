/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.onboarding

import java.io.Serializable

/**
 * Represents content for one page of Paper Onboarding
 */
class PaperOnboardingPage : Serializable {

    var titleText: String? = null
    var descriptionText: String? = null
    var bgColor: Int = 0
    var contentIconRes: Int = 0
    var bottomBarIconRes: Int = 0

    constructor()

    constructor(titleText: String, descriptionText: String, bgColor: Int, contentIconRes: Int, bottomBarIconRes: Int) {
        this.bgColor = bgColor
        this.contentIconRes = contentIconRes
        this.bottomBarIconRes = bottomBarIconRes
        this.descriptionText = descriptionText
        this.titleText = titleText
    }


    override fun toString(): String {
        return "PaperOnboardingPage(titleText=$titleText, descriptionText=$descriptionText, bgColor=$bgColor, contentIconRes=$contentIconRes, bottomBarIconRes=$bottomBarIconRes)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaperOnboardingPage) return false

        if (titleText != other.titleText) return false
        if (descriptionText != other.descriptionText) return false
        if (bgColor != other.bgColor) return false
        if (contentIconRes != other.contentIconRes) return false
        if (bottomBarIconRes != other.bottomBarIconRes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = titleText?.hashCode() ?: 0
        result = 31 * result + (descriptionText?.hashCode() ?: 0)
        result = 31 * result + bgColor
        result = 31 * result + contentIconRes
        result = 31 * result + bottomBarIconRes
        return result
    }


}
