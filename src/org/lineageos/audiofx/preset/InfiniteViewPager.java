/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.preset;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.lineageos.audiofx.R;
import org.lineageos.audiofx.activity.EqualizerManager;
import org.lineageos.audiofx.activity.MasterConfigControl;
import org.lineageos.audiofx.widget.WheelGrab;

/**
 * A {@link ViewPager} that allows pseudo-infinite paging with a wrap-around effect. Should be used
 * with an {@link InfinitePagerAdapter}.
 */
public class InfiniteViewPager extends ViewPager {

    private final EqualizerManager mEqManager;
    /** The wheel UI's side button grabs the presets, the wheel's steps then page through them */
    private final WheelGrab mWheelGrab = new WheelGrab(this,
            direction -> setCurrentItemAbsolute(getCurrentItem() + direction, true));

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public InfiniteViewPager(Context context) {
        super(context);
        mEqManager = MasterConfigControl.getInstance(context).getEqualizerManager();
    }

    public InfiniteViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEqManager = MasterConfigControl.getInstance(context).getEqualizerManager();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mEqManager.isAnimatingToCustom()) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int textSize = getResources().getDimensionPixelSize(R.dimen.preset_text_size)
                + getResources().getDimensionPixelSize(R.dimen.preset_text_padding)
                + 2 * getResources().getDimensionPixelSize(R.dimen.preset_text_padding_vertical);
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(textSize, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mEqManager.isAnimatingToCustom()) {
            return false;
        }
        boolean result;
        try {
            result = super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            /* There's a bug with the support library where it doesn't check
             * the proper pointer index, so when multi touching the container,
             * this can sometimes be thrown. Supposedly there are no downsides to just
             * catching the exception and moving along, so let's do that....
             */
            result = false;
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return getAdapter() != null && !mEqManager.isAnimatingToCustom()
                && mWheelGrab.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            mWheelGrab.letGo();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mWheelGrab.letGo();
        super.onDetachedFromWindow();
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);
        // offset first element so that we can scroll to the left
        setCurrentItem(0);
    }

    @Override
    public void setCurrentItem(int item) {
        // offset the current item to ensure there is space to scroll
        item = getOffsetAmount() + (item % getAdapter().getCount());
        super.setCurrentItem(item);
    }

    public void setCurrentItemAbsolute(int item) {
        super.setCurrentItem(item);
    }

    private int getOffsetAmount() {
        if (getAdapter() instanceof InfinitePagerAdapter) {
            InfinitePagerAdapter infAdapter = (InfinitePagerAdapter) getAdapter();
            // allow for 100 back cycles from the beginning
            // should be enough to create an illusion of infinity
            // warning: scrolling to very high values (1,000,000+) results in
            // strange drawing behaviour
            return infAdapter.getRealCount() * 100;
        } else {
            return 0;
        }
    }

    public void setCurrentItemAbsolute(int newPage, boolean b) {
        super.setCurrentItem(newPage, b);
    }
}
