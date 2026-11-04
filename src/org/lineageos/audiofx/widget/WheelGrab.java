/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.widget;

import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.View;
import android.widget.WheelCue;

/**
 * The wheel UI's hold on a control, as the system's sliders have: the side button's press (Enter)
 * grabs the focused view, the wheel's steps (up and down) then set it, and a press, a pause or the
 * focus leaving lets it go. Off where the system runs no wheel UI.
 */
public final class WheelGrab {

    /** What the grabbed view does with the wheel. */
    public interface Target {
        /** A step of the wheel: 1 clockwise (down), -1 back. */
        void onWheelStep(int direction);

        /** The view was grabbed or let go. */
        default void onWheelGrabChanged(boolean grabbed) {
        }
    }

    /** How long a grabbed view waits for the wheel before letting go. */
    private static final long TIMEOUT_MS = 4000;

    private final View mView;
    private final Target mTarget;
    private final Runnable mLetGo = () -> setGrabbed(false);
    private boolean mGrabbed;

    public WheelGrab(View view, Target target) {
        mView = view;
        mTarget = target;
    }

    /** Whether the system runs the wheel UI, where a device's wheel moves the focus. */
    public static boolean isWheelUi(View view) {
        final Resources res = view.getResources();
        final int id = res.getIdentifier("config_wheelFocus", "bool", "android");
        return id != 0 && res.getBoolean(id);
    }

    /** For the view's onKeyDown: whether the grab took the key. */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isWheelUi(mView)) {
            return false;
        }
        if (KeyEvent.isConfirmKey(keyCode)) {
            if (event.getRepeatCount() == 0) {
                setGrabbed(!mGrabbed);
            }
            return true;
        }
        if (!mGrabbed
                || keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_UP) {
            return false;
        }
        mView.removeCallbacks(mLetGo);
        mView.postDelayed(mLetGo, TIMEOUT_MS);
        mTarget.onWheelStep(keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1);
        return true;
    }

    /** For the view's onFocusChanged and onDetachedFromWindow: lets go. */
    public void letGo() {
        setGrabbed(false);
    }

    private void setGrabbed(boolean grabbed) {
        mView.removeCallbacks(mLetGo);
        if (grabbed == mGrabbed) {
            return;
        }
        mGrabbed = grabbed;
        // The wheel's ring shows it holds the view, as on the system's sliders
        mView.setPressed(grabbed);
        if (grabbed) {
            mView.postDelayed(mLetGo, TIMEOUT_MS);
            // The system sliders' own cue beside the wheel, word for word
            final Resources res = mView.getResources();
            WheelCue.show(mView, res.getIdentifier("wheel_cue_set", "string", "android"),
                    res.getIdentifier("wheel_cue_let_go", "string", "android"));
        } else {
            WheelCue.hide();
        }
        mTarget.onWheelGrabChanged(grabbed);
    }
}
