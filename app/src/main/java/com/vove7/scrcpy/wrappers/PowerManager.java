package com.vove7.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IInterface;

import com.vove7.scrcpy.Ln;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PowerManager extends IManager {

    public PowerManager(IInterface manager) {
        super(manager);
    }

    private Method getIsScreenOnMethod() throws NoSuchMethodException {

            @SuppressLint("ObsoleteSdkInt") // we may lower minSdkVersion in the future
            String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH ? "isInteractive" : "isScreenOn";
            return getMethod(methodName);


    }

    public boolean isScreenOn() {
        try {
            Method method = getIsScreenOnMethod();
            return (boolean) method.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
            return false;
        }
    }

    public boolean setPowerSaveModeEnabled(boolean mode) {
        try {
            return (boolean) getMethod("setPowerSaveModeEnabled", boolean.class).invoke(manager, mode);
        } catch (Exception e) {
            Ln.e("Could not invoke method", e);
            return false;
        }
    }
}
