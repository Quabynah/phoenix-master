/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.onboarding

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import io.pergasus.R
import java.util.*

/**
 * Main constructor for create a Paper Onboarding Engine
 *
 * @param rootLayout      root paper onboarding layout element
 * @param contentElements ordered list of prepared content elements for onboarding
 * @param appContext      application context
 */
class PaperOnboardingEngine(rootLayout: View, contentElements: ArrayList<PaperOnboardingPage>?, appContext: Context) : PaperOnboardingEngineDefaults {
    // scale factor for converting dp to px
    private val dpToPixelsScaleFactor: Float

    // main layout parts
    private val mRootLayout: RelativeLayout
    private val mContentTextContainer: FrameLayout
    private val mContentIconContainer: FrameLayout
    private val mBackgroundContainer: FrameLayout
    private val mPagerIconsContainer: LinearLayout

    private val mContentRootLayout: RelativeLayout
    private val mContentCenteredContainer: LinearLayout

    // application context
    private val mAppContext: Context

    // state variables
    private val mElements = ArrayList<PaperOnboardingPage>(0)
    /**
     * @return index of currently active element
     */
    var activeElementIndex = 0
        private set

    // params for Pager position calculations, virtually final, but initializes in onGlobalLayoutListener
    private var mPagerElementActiveSize: Int = 0
    private var mPagerElementNormalSize: Int = 0
    private var mPagerElementLeftMargin: Int = 0
    private var mPagerElementRightMargin: Int = 0

    // Listeners
    private var mOnChangeListener: PaperOnboardingOnChangeListener? = null
    private var mOnRightOutListener: PaperOnboardingOnRightOutListener? = null
    private var mOnLeftOutListener: PaperOnboardingOnLeftOutListener? = null

    /**
     * Returns content for currently active element
     *
     * @return content for currently active element
     */
    protected val activeElement: PaperOnboardingPage?
        get() = if (mElements.size > activeElementIndex) mElements[activeElementIndex] else null

    init {
        if (contentElements == null || contentElements.isEmpty())
            throw IllegalArgumentException("No content elements provided")

        this.mElements.addAll(contentElements)
        this.mAppContext = appContext.applicationContext

        mRootLayout = rootLayout as RelativeLayout
        mContentTextContainer = rootLayout.findViewById<View>(R.id.onboardingContentTextContainer) as FrameLayout
        mContentIconContainer = rootLayout.findViewById<View>(R.id.onboardingContentIconContainer) as FrameLayout
        mBackgroundContainer = rootLayout.findViewById<View>(R.id.onboardingBackgroundContainer) as FrameLayout
        mPagerIconsContainer = rootLayout.findViewById<View>(R.id.onboardingPagerIconsContainer) as LinearLayout

        mContentRootLayout = mRootLayout.getChildAt(1) as RelativeLayout
        mContentCenteredContainer = mContentRootLayout.getChildAt(0) as LinearLayout

        this.dpToPixelsScaleFactor = this.mAppContext.resources.displayMetrics.density

        initializeStartingState()

        mRootLayout.setOnTouchListener(object : OnSwipeListener(mAppContext) {
            override fun onSwipeLeft() {
                toggleContent(false)
            }

            override fun onSwipeRight() {
                toggleContent(true)
            }

        })

        mRootLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mRootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    mRootLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }

                mPagerElementActiveSize = mPagerIconsContainer.height
                mPagerElementNormalSize = Math.min(mPagerIconsContainer.getChildAt(0).height,
                        mPagerIconsContainer.getChildAt(mPagerIconsContainer.childCount - 1).height)

                val layoutParams = mPagerIconsContainer.getChildAt(0).layoutParams as ViewGroup.MarginLayoutParams
                mPagerElementLeftMargin = layoutParams.leftMargin
                mPagerElementRightMargin = layoutParams.rightMargin

                mPagerIconsContainer.x = calculateNewPagerPosition(0).toFloat()
                mContentCenteredContainer.y = ((mContentRootLayout.height - mContentCenteredContainer.height) / 2).toFloat()

            }
        })
    }

    /**
     * Calculate new position for pager without using pager's current position(like .getX())
     * this method allows to avoid incorrect position values while animation of pager in progress
     *
     * @param newActiveElement index of newly active element (from 0)
     * @return new X position for pager bar
     */
    protected fun calculateNewPagerPosition(newActiveElement: Int): Int {
        var newActiveElement = newActiveElement
        newActiveElement++
        if (newActiveElement <= 0)
            newActiveElement = 1
        val pagerActiveElemCenterPosX = (mPagerElementActiveSize / 2
                + newActiveElement * mPagerElementLeftMargin
                + (newActiveElement - 1) * (mPagerElementNormalSize + mPagerElementRightMargin))
        return mRootLayout.width / 2 - pagerActiveElemCenterPosX
    }

    /**
     * Calculate current center coordinates of pager element with provided index
     *
     * @param activeElementIndex index of element (from 0)
     * @return array with 2 coordinate values [x,y]
     */
    protected fun calculateCurrentCenterCoordinatesOfPagerElement(activeElementIndex: Int): IntArray {
        val y = (mPagerIconsContainer.y + mPagerIconsContainer.height / 2).toInt()

        if (activeElementIndex >= mPagerIconsContainer.childCount)
            return intArrayOf(mRootLayout.width / 2, y)

        val pagerElem = mPagerIconsContainer.getChildAt(activeElementIndex)
        val x = (mPagerIconsContainer.x + pagerElem.x + (pagerElem.width / 2).toFloat()).toInt()
        return intArrayOf(x, y)
    }

    /**
     * Initializes starting state
     */
    protected fun initializeStartingState() {
        // Create bottom bar icons for all elements with big first icon
        for (i in mElements.indices) {
            val PaperOnboardingPage = mElements[i]
            val bottomBarIconElement = createPagerIconElement(PaperOnboardingPage.bottomBarIconRes, i == 0)
            mPagerIconsContainer.addView(bottomBarIconElement)
        }
        // Initialize first element on screen
        val activeElement = activeElement
        // initial content texts
        val initialContentText = createContentTextView(activeElement)
        mContentTextContainer.addView(initialContentText)
        // initial content icons
        val initContentIcon = createContentIconView(activeElement)
        mContentIconContainer.addView(initContentIcon)
        // initial bg color
        mRootLayout.setBackgroundColor(activeElement!!.bgColor)
    }

    /**
     * @param prev set true to animate onto previous content page (default is false - animating to next content page)
     */
    protected fun toggleContent(prev: Boolean) {
        val oldElementIndex = activeElementIndex
        val newElement = if (prev) toggleToPreviousElement() else toggleToNextElement()

        if (newElement == null) {
            if (prev && mOnLeftOutListener != null)
                mOnLeftOutListener!!.onLeftOut()
            if (!prev && mOnRightOutListener != null)
                mOnRightOutListener!!.onRightOut()
            return
        }

        val newPagerPosX = calculateNewPagerPosition(activeElementIndex)

        // 1 - animate BG
        val bgAnimation = createBGAnimatorSet(newElement.bgColor)

        // 2 - animate pager position
        val pagerMoveAnimation = ObjectAnimator.ofFloat(mPagerIconsContainer, "x",
                mPagerIconsContainer.x, newPagerPosX.toFloat())
        pagerMoveAnimation.duration = PaperOnboardingEngineDefaults.ANIM_PAGER_BAR_MOVE_TIME.toLong()

        // 3 - animate pager icons
        val pagerIconAnimation = createPagerIconAnimation(oldElementIndex, activeElementIndex)

        // 4 animate content text
        val newContentText = createContentTextView(newElement)
        mContentTextContainer.addView(newContentText)
        val contentTextShowAnimation = createContentTextShowAnimation(
                mContentTextContainer.getChildAt(mContentTextContainer.childCount - 2), newContentText)

        // 5 animate content icon
        val newContentIcon = createContentIconView(newElement)
        mContentIconContainer.addView(newContentIcon)
        val contentIconShowAnimation = createContentIconShowAnimation(
                mContentIconContainer.getChildAt(mContentIconContainer.childCount - 2), newContentIcon)

        // 6 animate centering of all content
        val centerContentAnimation = createContentCenteringVerticalAnimation(newContentText, newContentIcon)

        centerContentAnimation.start()
        bgAnimation.start()
        pagerMoveAnimation.start()
        pagerIconAnimation.start()
        contentIconShowAnimation.start()
        contentTextShowAnimation.start()

        if (mOnChangeListener != null)
            mOnChangeListener!!.onPageChanged(oldElementIndex, activeElementIndex)
    }

    fun setOnChangeListener(onChangeListener: PaperOnboardingOnChangeListener) {
        this.mOnChangeListener = onChangeListener
    }

    fun setOnRightOutListener(onRightOutListener: PaperOnboardingOnRightOutListener) {
        this.mOnRightOutListener = onRightOutListener
    }

    fun setOnLeftOutListener(onLeftOutListener: PaperOnboardingOnLeftOutListener) {
        this.mOnLeftOutListener = onLeftOutListener
    }

    /**
     * @param color new background color for new
     * @return animator set with background color circular reveal animation
     */
    protected fun createBGAnimatorSet(color: Int): AnimatorSet {
        val bgColorView = ImageView(mAppContext)
        bgColorView.layoutParams = RelativeLayout.LayoutParams(mRootLayout.width, mRootLayout.height)
        bgColorView.setBackgroundColor(color)
        mBackgroundContainer.addView(bgColorView)

        val pos = calculateCurrentCenterCoordinatesOfPagerElement(activeElementIndex)

        val finalRadius = (if (mRootLayout.width > mRootLayout.height) mRootLayout.width else mRootLayout.height).toFloat()

        val bgAnimSet = AnimatorSet()
        val fadeIn = ObjectAnimator.ofFloat(bgColorView, "alpha", 0f, 1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val circularReveal = ViewAnimationUtils.createCircularReveal(bgColorView, pos[0], pos[1], 0f, finalRadius)
            circularReveal.interpolator = AccelerateInterpolator()
            bgAnimSet.playTogether(circularReveal, fadeIn)
        } else {
            bgAnimSet.playTogether(fadeIn)
        }

        bgAnimSet.duration = PaperOnboardingEngineDefaults.ANIM_BACKGROUND_TIME.toLong()
        bgAnimSet.addListener(object : AnimatorEndListener() {
            override fun onAnimationEnd(animation: Animator) {
                mRootLayout.setBackgroundColor(color)
                bgColorView.visibility = View.GONE
                mBackgroundContainer.removeView(bgColorView)
            }
        })
        return bgAnimSet
    }

    /**
     * @param currentContentText currently displayed view with text
     * @param newContentText     newly created and prepared view to display
     * @return animator set with this animation
     */
    private fun createContentTextShowAnimation(currentContentText: View, newContentText: View): AnimatorSet {
        val positionDeltaPx = dpToPixels(PaperOnboardingEngineDefaults.CONTENT_TEXT_POS_DELTA_Y_DP)
        val animations = AnimatorSet()
        val currentContentMoveUp = ObjectAnimator.ofFloat(currentContentText, "y", 0f,
                -positionDeltaPx.toFloat())
        currentContentMoveUp.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_TEXT_HIDE_TIME.toLong()
        currentContentMoveUp.addListener(object : AnimatorEndListener() {
            override fun onAnimationEnd(animation: Animator) {
                mContentTextContainer.removeView(currentContentText)
            }
        })
        val currentContentFadeOut = ObjectAnimator.ofFloat(currentContentText, "alpha", 1f, 0f)
        currentContentFadeOut.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_TEXT_HIDE_TIME.toLong()

        animations.playTogether(currentContentMoveUp, currentContentFadeOut)

        val newContentMoveUp = ObjectAnimator.ofFloat(newContentText, "y", positionDeltaPx.toFloat(), 0f)
        newContentMoveUp.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_TEXT_SHOW_TIME.toLong()

        val newContentFadeIn = ObjectAnimator.ofFloat(newContentText, "alpha", 0f, 1f)
        newContentFadeIn.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_TEXT_SHOW_TIME.toLong()

        animations.playTogether(newContentMoveUp, newContentFadeIn)

        animations.interpolator = DecelerateInterpolator()

        return animations
    }

    /**
     * @param currentContentIcon currently displayed view with icon
     * @param newContentIcon     newly created and prepared view to display
     * @return animator set with this animation
     */
    protected fun createContentIconShowAnimation(currentContentIcon: View, newContentIcon: View): AnimatorSet {
        val positionDeltaPx = dpToPixels(PaperOnboardingEngineDefaults.CONTENT_ICON_POS_DELTA_Y_DP)
        val animations = AnimatorSet()
        val currentContentMoveUp = ObjectAnimator.ofFloat(currentContentIcon, "y", 0f,
                -positionDeltaPx.toFloat())
        currentContentMoveUp.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_ICON_HIDE_TIME.toLong()

        currentContentMoveUp.addListener(object : AnimatorEndListener() {
            override fun onAnimationEnd(animation: Animator) {
                mContentIconContainer.removeView(currentContentIcon)
            }
        })
        val currentContentFadeOut = ObjectAnimator.ofFloat(currentContentIcon, "alpha", 1f, 0f)
        currentContentFadeOut.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_ICON_HIDE_TIME.toLong()

        animations.playTogether(currentContentMoveUp, currentContentFadeOut)

        val newContentMoveUp = ObjectAnimator.ofFloat(newContentIcon, "y", positionDeltaPx.toFloat(), 0f)
        newContentMoveUp.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_ICON_SHOW_TIME.toLong()

        val newContentFadeIn = ObjectAnimator.ofFloat(newContentIcon, "alpha", 0f, 1f)
        newContentFadeIn.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_ICON_SHOW_TIME.toLong()

        animations.playTogether(newContentMoveUp, newContentFadeIn)

        animations.interpolator = DecelerateInterpolator()

        return animations
    }

    protected fun createContentCenteringVerticalAnimation(newContentText: View, newContentIcon: View): Animator {
        newContentText.measure(View.MeasureSpec.makeMeasureSpec(mContentCenteredContainer.width, View.MeasureSpec.AT_MOST), -2)
        val measuredContentTextHeight = newContentText.measuredHeight
        newContentIcon.measure(-2, -2)
        val measuredContentIconHeight = newContentIcon.measuredHeight

        val newHeightOfContent = measuredContentIconHeight + measuredContentTextHeight + (mContentTextContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val centerContentAnimation = ObjectAnimator.ofFloat(mContentCenteredContainer, "y", mContentCenteredContainer.y,
                (mContentRootLayout.height - newHeightOfContent.toFloat()) / 2)
        centerContentAnimation.duration = PaperOnboardingEngineDefaults.ANIM_CONTENT_CENTERING_TIME.toLong()
        centerContentAnimation.interpolator = DecelerateInterpolator()
        return centerContentAnimation
    }

    /**
     * Create animator for pager icon
     *
     * @param oldIndex index currently active icon
     * @param newIndex index of new active icon
     * @return animator set with this animation
     */
    protected fun createPagerIconAnimation(oldIndex: Int, newIndex: Int): AnimatorSet {
        val animations = AnimatorSet()
        animations.duration = PaperOnboardingEngineDefaults.ANIM_PAGER_ICON_TIME.toLong()

        // scale down whole old element
        val oldActiveItem = mPagerIconsContainer.getChildAt(oldIndex) as ViewGroup
        val oldActiveItemParams = oldActiveItem.layoutParams as LinearLayout.LayoutParams
        val oldItemScaleDown = ValueAnimator.ofInt(mPagerElementActiveSize, mPagerElementNormalSize)
        oldItemScaleDown.addUpdateListener { valueAnimator ->
            oldActiveItemParams.height = valueAnimator.animatedValue as Int
            oldActiveItemParams.width = valueAnimator.animatedValue as Int
            oldActiveItem.requestLayout()
        }

        // fade out old new element icon
        val oldActiveIcon = oldActiveItem.getChildAt(1)
        val oldActiveIconFadeOut = ObjectAnimator.ofFloat(oldActiveIcon, "alpha", 1f, 0f)

        // fade in old element shape
        val oldActiveShape = oldActiveItem.getChildAt(0) as ImageView
        oldActiveShape.setImageResource(if (oldIndex - newIndex > 0) R.drawable.onboarding_pager_circle_icon else R.drawable.onboarding_pager_round_icon)
        val oldActiveShapeFadeIn = ObjectAnimator.ofFloat(oldActiveShape, "alpha", 0f,
                PaperOnboardingEngineDefaults.PAGER_ICON_SHAPE_ALPHA)
        // add animations
        animations.playTogether(oldItemScaleDown, oldActiveIconFadeOut, oldActiveShapeFadeIn)

        // scale up whole new element
        val newActiveItem = mPagerIconsContainer.getChildAt(newIndex) as ViewGroup
        val newActiveItemParams = newActiveItem.layoutParams as LinearLayout.LayoutParams
        val newItemScaleUp = ValueAnimator.ofInt(mPagerElementNormalSize, mPagerElementActiveSize)
        newItemScaleUp.addUpdateListener { valueAnimator ->
            newActiveItemParams.height = valueAnimator.animatedValue as Int
            newActiveItemParams.width = valueAnimator.animatedValue as Int
            newActiveItem.requestLayout()
        }

        // fade in new element icon
        val newActiveIcon = newActiveItem.getChildAt(1)
        val newActiveIconFadeIn = ObjectAnimator.ofFloat(newActiveIcon, "alpha", 0f, 1f)

        // fade out new element shape
        val newActiveShape = newActiveItem.getChildAt(0) as ImageView
        val newActiveShapeFadeOut = ObjectAnimator.ofFloat(newActiveShape, "alpha",
                PaperOnboardingEngineDefaults.PAGER_ICON_SHAPE_ALPHA, 0f)

        // add animations
        animations.playTogether(newItemScaleUp, newActiveShapeFadeOut, newActiveIconFadeIn)

        animations.interpolator = DecelerateInterpolator()
        return animations
    }

    /**
     * @param iconDrawableRes drawable resource for icon
     * @param isActive        is active element
     * @return configured pager icon with selected drawable and selected state (active or inactive)
     */
    protected fun createPagerIconElement(iconDrawableRes: Int, isActive: Boolean): ViewGroup {
        val vi = LayoutInflater.from(mAppContext)
        val bottomBarElement = vi.inflate(R.layout.onboarding_pager_layout, mPagerIconsContainer, false) as FrameLayout
        val elementShape = bottomBarElement.getChildAt(0) as ImageView
        val elementIcon = bottomBarElement.getChildAt(1) as ImageView
        elementIcon.setImageResource(iconDrawableRes)
        if (isActive) {
            val layoutParams = bottomBarElement.layoutParams as LinearLayout.LayoutParams
            layoutParams.width = mPagerIconsContainer.layoutParams.height
            layoutParams.height = mPagerIconsContainer.layoutParams.height
            elementShape.alpha = 0f
            elementIcon.alpha = 1f
        } else {
            elementShape.alpha = PaperOnboardingEngineDefaults.PAGER_ICON_SHAPE_ALPHA
            elementIcon.alpha = 0f
        }
        return bottomBarElement
    }

    /**
     * @param PaperOnboardingPage new content page to show
     * @return configured view with new content texts
     */
    protected fun createContentTextView(PaperOnboardingPage: PaperOnboardingPage?): ViewGroup {
        val vi = LayoutInflater.from(mAppContext)
        val contentTextView = vi.inflate(R.layout.onboarding_text_content_layout, mContentTextContainer, false) as ViewGroup
        val contentTitle = contentTextView.getChildAt(0) as TextView
        contentTitle.text = PaperOnboardingPage!!.titleText
        val contentText = contentTextView.getChildAt(1) as TextView
        contentText.text = PaperOnboardingPage.descriptionText
        return contentTextView
    }

    /**
     * @param PaperOnboardingPage new content page to show
     * @return configured view with new content image
     */
    protected fun createContentIconView(PaperOnboardingPage: PaperOnboardingPage?): ImageView {
        val contentIcon = ImageView(mAppContext)
        contentIcon.setImageResource(PaperOnboardingPage!!.contentIconRes)
        val iconLP = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        iconLP.gravity = Gravity.CENTER
        contentIcon.layoutParams = iconLP
        return contentIcon
    }

    /**
     * Changes active element to the previous one and returns a new content
     *
     * @return content for previous element
     */
    protected fun toggleToPreviousElement(): PaperOnboardingPage? {
        if (activeElementIndex - 1 >= 0) {
            activeElementIndex--
            return if (mElements.size > activeElementIndex) mElements[activeElementIndex] else null
        } else
            return null
    }

    /**
     * Changes active element to the next one and returns a new content
     *
     * @return content for next element
     */
    protected fun toggleToNextElement(): PaperOnboardingPage? {
        if (activeElementIndex + 1 < mElements.size) {
            activeElementIndex++
            return if (mElements.size > activeElementIndex) mElements[activeElementIndex] else null
        } else
            return null
    }

    /**
     * Converts DP values to PX
     *
     * @param dpValue value to convert in dp
     * @return converted value in px
     */
    protected fun dpToPixels(dpValue: Int): Int {
        return (dpValue * dpToPixelsScaleFactor + 0.5f).toInt()
    }
}
