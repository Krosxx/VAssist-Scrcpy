package com.vove7.scrcpy;

import android.view.InputEvent;

/**
 * @author Vove
 * @date 2021/8/21
 */
public interface DeviceOp {

    default boolean pressReleaseKeycode(int keyCode) {
        return false;
    }

    default void setDisplayId(int displayId) {
    }

    default int getLayerStack() {
        return -1;
    }

    default boolean supportsInputEvents() {
        return false;
    }

    default boolean injectEvent(InputEvent event) {
        return false;
    }

    default boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        return false;
    }
}
