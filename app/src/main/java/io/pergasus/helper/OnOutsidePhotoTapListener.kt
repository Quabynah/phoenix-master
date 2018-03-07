/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.helper

import android.widget.ImageView

/**
 * Callback when the user tapped outside of the photo
 */
interface OnOutsidePhotoTapListener {

    /**
     * The outside of the photo has been tapped
     */
    fun onOutsidePhotoTap(imageView: ImageView)
}

