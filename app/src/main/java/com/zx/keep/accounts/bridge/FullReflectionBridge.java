package com.zx.keep.accounts.bridge;

import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class FullReflectionBridge {
    private static final String TAG = "FullReflectionBridge";
    private static final ConcurrentHashMap<String, Object> instanceCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    public static String invoke(String json) {
        try {
            JSONObject req = new JSONObject(json);
            String className = req.getString("className");
            String methodName = req.optString("methodName", "");
            JSONArray paramsArray = req.optJSONArray("paramsArray");
            boolean isStatic = req.optBoolean("isStatic", true);

            SecurityManager.validate(className, methodName);

            Object[] params = paramsArray != null ? TypeInferer.infer(paramsArray) : new Object[0];
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramTypes[i] = params[i] != null ? params[i].getClass() : Object.class;
            }

            Class<?> clazz = Class.forName(className);
            Method method = findMethod(clazz, methodName, paramTypes, isStatic);

            JSONObject result = new JSONObject();
            if (isStatic) {
                Object ret = method.invoke(null, params);
                result.put("success", true);
                result.put("data", ret != null ? ret.toString() : JSONObject.NULL);
            } else {
                Object instance = getOrCreateInstance(clazz, className);
                Object ret = method.invoke(instance, params);
                result.put("success", true);
                result.put("data", ret != null ? ret.toString() : JSONObject.NULL);
            }
            return result.toString();

        } catch (Exception e) {
            Log.e(TAG, "Bridge invoke error: " + e.getMessage());
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err.toString();
            } catch (Exception ignored) {
                return "{\"success\":false,\"error\":\"unknown\"}";
            }
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, boolean isStatic)
            throws NoSuchMethodException {
        String key = clazz.getName() + "." + methodName + Arrays.toString(paramTypes) + isStatic;
        Method cached = methodCache.get(key);
        if (cached != null) return cached;

        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName)) {
                boolean matchStatic = Modifier.isStatic(m.getModifiers()) == isStatic;
                if (!matchStatic) continue;
                Class<?>[] mt = m.getParameterTypes();
                if (mt.length != paramTypes.length) continue;
                boolean match = true;
                for (int i = 0; i < mt.length; i++) {
                    if (!isAssignable(mt[i], paramTypes[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    methodCache.put(key, m);
                    return m;
                }
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }

    private static boolean isAssignable(Class<?> declared, Class<?> actual) {
        if (declared.isAssignableFrom(actual)) return true;
        if (declared == boolean.class && actual == Boolean.class) return true;
        if (declared == int.class && actual == Integer.class) return true;
        if (declared == long.class && actual == Long.class) return true;
        if (declared == float.class && actual == Float.class) return true;
        if (declared == double.class && actual == Double.class) return true;
        if (declared == short.class && actual == Short.class) return true;
        if (declared == byte.class && actual == Byte.class) return true;
        if (declared == char.class && actual == Character.class) return true;
        return false;
    }

    private static Object getOrCreateInstance(Class<?> clazz, String className) throws Exception {
        Object cached = instanceCache.get(className);
        if (cached != null) return cached;

        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        instanceCache.put(className, instance);
        return instance;
    }

    public static void clearCache() {
        instanceCache.clear();
        methodCache.clear();
    }
}
