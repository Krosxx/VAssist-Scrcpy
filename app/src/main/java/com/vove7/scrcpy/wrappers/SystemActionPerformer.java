
package com.vove7.scrcpy.wrappers;


import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.vove7.scrcpy.Device;
import com.vove7.scrcpy.Ln;
import com.vove7.scrcpy.Workarounds;

import androidx.annotation.RequiresApi;

public class SystemActionPerformer {

    public boolean performSystemAction(int actionId) {
        switch (actionId) {
            case AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS: {
                expandNotifications();
                return true;
            }
            case AccessibilityService.GLOBAL_ACTION_RECENTS:
                return openRecents();
            case AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS: {
                expandQuickSettings();
                return true;
            }
            case AccessibilityService.GLOBAL_ACTION_POWER_DIALOG: {
                return showGlobalActions();
            }
            case AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN:
                return toggleSplitScreen();
            case AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return takeScreenshot();
                }
                return false;
            default:
                return false;
        }
    }

    private boolean openRecents() {
        return Device.SERVICE_MANAGER.getStatusBarManager().toggleRecentApps();
    }

    private void expandQuickSettings() {
        Device.SERVICE_MANAGER.getStatusBarManager().expandSettingsPanel();
    }

    private boolean showGlobalActions() {
        return Device.SERVICE_MANAGER.getWindowManager().showGlobalActions();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean takeScreenshot() {
        //new ScreenshotHelper(Workarounds.SysContext).takeScreenshot(
        //        1, true, true, 4,
        //        new Handler(Looper.getMainLooper()), uri -> {
        //            Ln.e("takeScreenshot: " + uri);
        //        }
        //);
        return false;
    }

    private void expandNotifications() {
        Device.SERVICE_MANAGER.getStatusBarManager().expandNotificationsPanel();
    }

    //todo
    private boolean toggleSplitScreen() {
        //return Device.SERVICE_MANAGER.getStatusBarManager().toggleSplitScreen();
        return false;
    }

}