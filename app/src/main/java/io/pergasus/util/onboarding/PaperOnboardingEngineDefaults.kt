/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.onboarding

/**
 * Default values for everything
 */
interface PaperOnboardingEngineDefaults {
    companion object {
        const val TAG = "POB"

        // animation and view settings
        const val ANIM_PAGER_BAR_MOVE_TIME = 600

        const val ANIM_PAGER_ICON_TIME = 350

        const val ANIM_BACKGROUND_TIME = 450

        const val CONTENT_TEXT_POS_DELTA_Y_DP = 50
        const val ANIM_CONTENT_TEXT_SHOW_TIME = 800
        const val ANIM_CONTENT_TEXT_HIDE_TIME = 200

        const val CONTENT_ICON_POS_DELTA_Y_DP = 50
        const val ANIM_CONTENT_ICON_SHOW_TIME = 800
        const val ANIM_CONTENT_ICON_HIDE_TIME = 200

        const val PAGER_ICON_SHAPE_ALPHA = 0.5f

        const val ANIM_CONTENT_CENTERING_TIME = 800
    }

}
