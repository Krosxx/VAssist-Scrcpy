package com.vove7.scrcpy.wrappers;

import com.vove7.scrcpy.Ln;

import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StatusBarManager extends IManager {

    private final IInterface manager;
    private Method expandSettingsPanelMethod;
    private boolean expandSettingsPanelMethodNewVersion = true;
    private Method collapsePanelsMethod;

    public StatusBarManager(IInterface manager) {
        super(manager);
        this.manager = manager;
    }

    private Method getExpandSettingsPanel() throws NoSuchMethodException {
        if (expandSettingsPanelMethod == null) {
            try {
                // Since Android 7: https://android.googlesource.com/platform/frameworks/base.git/+/a9927325eda025504d59bb6594fee8e240d95b01%5E%21/
                expandSettingsPanelMethod = manager.getClass().getMethod("expandSettingsPanel", String.class);
            } catch (NoSuchMethodException e) {
                // old version
                expandSettingsPanelMethod = manager.getClass().getMethod("expandSettingsPanel");
                expandSettingsPanelMethodNewVersion = false;
            }
        }
        return expandSettingsPanelMethod;
    }

    private Method getCollapsePanelsMethod() throws NoSuchMethodException {
        if (collapsePanelsMethod == null) {
            collapsePanelsMethod = manager.getClass().getMethod("collapsePanels");
        }
        return collapsePanelsMethod;
    }

    public void expandNotificationsPanel() {
        try {
            Method method = getMethod("expandNotificationsPanel");
            method.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
    }

    public void expandSettingsPanel() {
        try {
            Method method = getExpandSettingsPanel();
            if (expandSettingsPanelMethodNewVersion) {
                // new version
                method.invoke(manager, (Object) null);
            } else {
                // old version
                method.invoke(manager);
            }
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
    }

    public void collapsePanels() {
        try {
            Method method = getCollapsePanelsMethod();
            method.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
    }

    public boolean toggleRecentApps() {
        try {
            getMethod("toggleRecentApps").invoke(manager);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
    }

}
