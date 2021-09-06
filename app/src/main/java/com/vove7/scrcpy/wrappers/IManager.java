package com.vove7.scrcpy.wrappers;

import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vove
 * @date 2021/8/22
 */
abstract class IManager {
    protected final IInterface manager;

    private final Map<String, Method> CachedMethods = new HashMap<>();

    protected IManager(IInterface manager) {
        this.manager = manager;
    }

    Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        String mid = methodIds(name, parameterTypes);
        Method cm = CachedMethods.get(mid);
        if (cm == null) {
            cm = manager.getClass().getMethod(name, parameterTypes);
            CachedMethods.put(mid, cm);
        }

        return cm;
    }

    private String methodIds(String name, Class<?>... parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":");
        for (Class<?> c : parameterTypes) {
            sb.append(c.getName()).append("|");
        }
        return sb.toString();
    }
}
