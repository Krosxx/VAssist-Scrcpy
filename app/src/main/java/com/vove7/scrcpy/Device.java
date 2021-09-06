package com.vove7.scrcpy;

import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.vove7.scrcpy.wrappers.ClipboardManager;
import com.vove7.scrcpy.wrappers.ContentProvider;
import com.vove7.scrcpy.wrappers.InputManager;
import com.vove7.scrcpy.wrappers.ServiceManager;
import com.vove7.scrcpy.wrappers.SurfaceControl;
import com.vove7.scrcpy.wrappers.WindowManager;

import static java.lang.Thread.sleep;

public final class Device implements DeviceOp {

    public static final int POWER_MODE_OFF = SurfaceControl.POWER_MODE_OFF;
    public static final int POWER_MODE_NORMAL = SurfaceControl.POWER_MODE_NORMAL;

    public static final int LOCK_VIDEO_ORIENTATION_UNLOCKED = -1;
    public static final int LOCK_VIDEO_ORIENTATION_INITIAL = -2;

    private static final ServiceManager SERVICE_MANAGER = new ServiceManager();

    /**
     * The surface flinger layer stack associated with this logical display
     */
    private final int layerStack;

    private final boolean supportsInputEvents;

    private int displayId = 0;

    public Device() {
        DisplayInfo displayInfo = SERVICE_MANAGER.getDisplayManager().getDisplayInfo(displayId);
        if (displayInfo == null) {
            int[] displayIds = SERVICE_MANAGER.getDisplayManager().getDisplayIds();
            throw new InvalidDisplayIdException(displayId, displayIds);
        }

        int displayInfoFlags = displayInfo.getFlags();

        layerStack = displayInfo.getLayerStack();

        if ((displayInfoFlags & DisplayInfo.FLAG_SUPPORTS_PROTECTED_BUFFERS) == 0) {
            Ln.w("Display doesn't have FLAG_SUPPORTS_PROTECTED_BUFFERS flag, mirroring can be restricted");
        }

        // main display or any display on Android >= Q
        supportsInputEvents = displayId == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        if (!supportsInputEvents) {
            Ln.w("Input events are not supported for secondary displays before Android 10");
        }
    }

    public void setDisplayId(int displayId) {
        this.displayId = displayId;
    }

    public int getLayerStack() {
        return layerStack;
    }

    public static String getDeviceName() {
        return Build.MODEL;
    }

    public static boolean supportsInputEvents(int displayId) {
        return displayId == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public boolean supportsInputEvents() {
        return supportsInputEvents;
    }

    public static boolean injectEvent(InputEvent inputEvent, int displayId) {
        if (!supportsInputEvents(displayId)) {
            throw new AssertionError("Could not inject input event if !supportsInputEvents()");
        }

        if (displayId != 0 && !InputManager.setDisplayId(inputEvent, displayId)) {
            return false;
        }

        return SERVICE_MANAGER.getInputManager()
                .injectInputEvent(inputEvent,
                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectEvent(InputEvent event) {
        return injectEvent(event, displayId);
    }

    public static boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState, int displayId) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event, displayId);
    }

    public boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        return injectKeyEvent(action, keyCode, repeat, metaState, displayId);
    }

    public static boolean pressReleaseKeycode(int keyCode, int displayId) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0, displayId) && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0, displayId);
    }

    public boolean pressReleaseKeycode(int keyCode) {
        return pressReleaseKeycode(keyCode, displayId);
    }

    public static boolean isScreenOn() {
        return SERVICE_MANAGER.getPowerManager().isScreenOn();
    }

    public static void expandNotificationPanel() {
        SERVICE_MANAGER.getStatusBarManager().expandNotificationsPanel();
    }

    public static void expandSettingsPanel() {
        SERVICE_MANAGER.getStatusBarManager().expandSettingsPanel();
    }

    public static void collapsePanels() {
        SERVICE_MANAGER.getStatusBarManager().collapsePanels();
    }

    public static String getClipboardText() {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return null;
        }
        CharSequence s = clipboardManager.getText();
        if (s == null) {
            return null;
        }
        return s.toString();
    }

    public static boolean setClipboardText(String text) {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return false;
        }
        return clipboardManager.setText(text);
    }

    /**
     * @param mode one of the {@code POWER_MODE_*} constants
     */
    public static boolean setScreenPowerMode(int mode) {
        IBinder d = SurfaceControl.getBuiltInDisplay();
        if (d == null) {
            Ln.e("Could not get built-in display");
            return false;
        }
        return SurfaceControl.setDisplayPowerMode(d, mode);
    }

    public static boolean powerOffScreen(int displayId) {
        if (!isScreenOn()) {
            return true;
        }
        return pressReleaseKeycode(KeyEvent.KEYCODE_POWER, displayId);
    }

    /**
     * Disable auto-rotation (if enabled), set the screen rotation and re-enable auto-rotation (if it was enabled).
     */
    public static void rotateDevice() {
        WindowManager wm = SERVICE_MANAGER.getWindowManager();

        boolean accelerometerRotation = !wm.isRotationFrozen();

        int currentRotation = wm.getRotation();
        int newRotation = (currentRotation & 1) ^ 1; // 0->1, 1->0, 2->1, 3->0
        String newRotationString = newRotation == 0 ? "portrait" : "landscape";

        Ln.i("Device rotation requested: " + newRotationString);
        wm.freezeRotation(newRotation);

        // restore auto-rotate if necessary
        if (accelerometerRotation) {
            wm.thawRotation();
        }
    }

    public static ContentProvider createSettingsProvider() {
        return SERVICE_MANAGER.getActivityManager().createSettingsProvider();
    }

    public static boolean setPowerSaveModeEnabled(boolean mode) {
        return SERVICE_MANAGER.getPowerManager().setPowerSaveModeEnabled(mode);
    }
}
