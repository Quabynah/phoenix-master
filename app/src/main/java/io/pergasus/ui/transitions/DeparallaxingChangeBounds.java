/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui.transitions;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.ViewGroup;

import io.pergasus.ui.widget.ParallaxScrimageView;

/**
 * An extension to {@link ChangeBounds} designed to work with {@link ParallaxScrimageView}. This
 * will remove any parallax applied while also performing a {@code ChangeBounds} transition.
 */
public class DeparallaxingChangeBounds extends ChangeBounds {

    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";

    public DeparallaxingChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        if (!(transitionValues.view instanceof ParallaxScrimageView)) return;
        ParallaxScrimageView psv = ((ParallaxScrimageView) transitionValues.view);
        if (psv.getOffset() == 0) return;

        // as we're going to remove the offset (which drives the parallax) we need to
        // compensate for this by adjusting the target bounds.
        Rect bounds = (Rect) transitionValues.values.get(PROPNAME_BOUNDS);
        bounds.offset(0, psv.getOffset());
        transitionValues.values.put(PROPNAME_BOUNDS, bounds);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot,
                                   TransitionValues startValues,
                                   TransitionValues endValues) {
        Animator changeBounds = super.createAnimator(sceneRoot, startValues, endValues);
        if (startValues == null || endValues == null
                || !(endValues.view instanceof ParallaxScrimageView)) return changeBounds;
        ParallaxScrimageView psv = ((ParallaxScrimageView) endValues.view);
        if (psv.getOffset() == 0) return changeBounds;

        Animator deparallax = ObjectAnimator.ofInt(psv, ParallaxScrimageView.OFFSET, 0);
        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(changeBounds, deparallax);
        return transition;
    }
}
