package com.zx.keep.accounts;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import com.zx.keep.accounts.security.AuditLogger;
import com.zx.keep.accounts.security.SecurityConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSBridge {
    private static final String TAG = "JSBridge";
    private final ConcurrentHashMap<String, Object> instanceCache = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @JavascriptInterface
    public String invoke(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String className = json.optString("className");
            String methodName = json.optString("methodName");
            JSONArray paramsArray = json.optJSONArray("paramsArray");
            boolean isStatic = json.optBoolean("isStatic", true);
            String instanceKey = json.optString("instanceKey", null);

            Object[] args;
            Class<?>[] argTypes;
            if (paramsArray == null || paramsArray.length() <= 0) {
                args = new Object[0];
                argTypes = new Class[0];
            } else {
                args = new Object[paramsArray.length()];
                argTypes = new Class[paramsArray.length()];
                for (int i = 0; i < paramsArray.length(); i++) {
                    Object converted = convertJsonToJava(paramsArray.get(i));
                    args[i] = converted;
                    argTypes[i] = converted != null ? converted.getClass() : Object.class;
                }
            }

            AuditLogger.logInvoke(className, methodName,
                    paramsArray != null ? paramsArray.toString() : "[]");

            if (!SecurityConfig.isClassAllowed(className)) {
                AuditLogger.logSecurityBlock("Class Not Allowed", className);
                return errorResult("Security blocked: class '" + className + "' is not in whitelist");
            }
            if (SecurityConfig.isMethodBlocked(methodName)) {
                AuditLogger.logSecurityBlock("Method Blocked", className + "." + methodName);
                return errorResult("Security blocked: method '" + methodName + "' is in blacklist");
            }

            try {
                Class<?> cls = Class.forName(className);
                Object result;
                if (isStatic) {
                    result = invokeStaticMethod(cls, methodName, args, argTypes);
                } else {
                    result = invokeInstanceMethod(cls, methodName, args, argTypes, instanceKey);
                }
                return successResult(result);
            } catch (ClassNotFoundException e) {
                return errorResult("Class not found: " + className);
            }
        } catch (JSONException e) {
            return errorResult("JSON parse error: " + e.getMessage());
        } catch (Exception e) {
            AuditLogger.logError(TAG, e.getMessage());
            return errorResult("Invoke error: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public String createInstance(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String className = json.optString("className");
            JSONArray constructorArgs = json.optJSONArray("constructorArgs");
            String instanceKey = json.optString("instanceKey", className);

            if (!SecurityConfig.isClassAllowed(className)) {
                return errorResult("Security blocked: class not allowed");
            }

            Class<?> cls = Class.forName(className);
            Object[] args;
            Class<?>[] argTypes;
            if (constructorArgs == null || constructorArgs.length() <= 0) {
                args = new Object[0];
                argTypes = new Class[0];
            } else {
                args = new Object[constructorArgs.length()];
                argTypes = new Class[constructorArgs.length()];
                for (int i = 0; i < constructorArgs.length(); i++) {
                    Object converted = convertJsonToJava(constructorArgs.get(i));
                    args[i] = converted;
                    argTypes[i] = converted != null ? converted.getClass() : Object.class;
                }
            }

            Constructor<?> constructor = cls.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            this.instanceCache.put(instanceKey, constructor.newInstance(args));
            AuditLogger.logInvoke(className, "<init>", instanceKey);

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("instanceKey", instanceKey);
            result.put("className", className);
            return result.toString();
        } catch (Exception e) {
            AuditLogger.logError(TAG, "createInstance: " + e.getMessage());
            return errorResult("Create instance error: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public String destroyInstance(String instanceKey) {
        Object removed = this.instanceCache.remove(instanceKey);
        JSONObject result = new JSONObject();
        try {
            result.put("success", removed != null);
            result.put("instanceKey", instanceKey);
            return result.toString();
        } catch (JSONException e) {
            return errorResult(e.getMessage());
        }
    }

    private Object invokeStaticMethod(Class<?> cls, String methodName, Object[] args, Class<?>[] argTypes) throws Exception {
        Method method = findMethod(cls, methodName, argTypes);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + cls.getName() + "." + methodName);
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalAccessException("Method is not static: " + methodName);
        }
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private Object invokeInstanceMethod(Class<?> cls, String methodName, Object[] args, Class<?>[] argTypes, String instanceKey) throws Exception {
        Object instance = instanceKey != null ? this.instanceCache.get(instanceKey) : null;
        if (instance == null) {
            instance = ModuleRegistry.get(cls.getName());
        }
        if (instance == null) {
            throw new IllegalStateException("No instance found for key: " + instanceKey +
                    ". Call createInstance first or register the module.");
        }
        Method method = findMethod(cls, methodName, argTypes);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + cls.getName() + "." + methodName);
        }
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    private Method findMethod(Class<?> cls, String methodName, Class<?>[] argTypes) {
        try {
            return cls.getDeclaredMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == argTypes.length) {
                        boolean match = true;
                        for (int i = 0; i < paramTypes.length; i++) {
                            Class<?> pt = paramTypes[i];
                            Class<?> at = argTypes[i];
                            if (at != null && !isAssignable(pt, at)) {
                                match = false;
                                break;
                            }
                        }
                        if (match) return method;
                    }
                }
            }
            try {
                return cls.getMethod(methodName, argTypes);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }

    private boolean isAssignable(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) return true;
        if (target.isPrimitive()) {
            Class<?> wrapper = getWrapperClass(target);
            return wrapper != null && wrapper.isAssignableFrom(source);
        }
        if (!source.isPrimitive() && !isWrapperClass(source)) return false;
        Class<?> primitive = getPrimitiveClass(source);
        return primitive != null && primitive == target;
    }

    private Object convertJsonToJava(Object obj) throws JSONException {
        if (obj == null || obj == JSONObject.NULL) return null;
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            if (json.has("__type") && "byteArray".equals(json.getString("__type"))) {
                return Base64.decode(json.getString("value"), Base64.DEFAULT);
            }
            return json;
        }
        if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            Object[] result = new Object[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                result[i] = convertJsonToJava(arr.get(i));
            }
            return result;
        }
        return obj;
    }

    private Class<?> getWrapperClass(Class<?> primitive) {
        if (primitive == Integer.TYPE) return Integer.class;
        if (primitive == Long.TYPE) return Long.class;
        if (primitive == Float.TYPE) return Float.class;
        if (primitive == Double.TYPE) return Double.class;
        if (primitive == Boolean.TYPE) return Boolean.class;
        if (primitive == Byte.TYPE) return Byte.class;
        if (primitive == Short.TYPE) return Short.class;
        if (primitive == Character.TYPE) return Character.class;
        return null;
    }

    private Class<?> getPrimitiveClass(Class<?> wrapper) {
        if (wrapper == Integer.class) return Integer.TYPE;
        if (wrapper == Long.class) return Long.TYPE;
        if (wrapper == Float.class) return Float.TYPE;
        if (wrapper == Double.class) return Double.TYPE;
        if (wrapper == Boolean.class) return Boolean.TYPE;
        if (wrapper == Byte.class) return Byte.TYPE;
        if (wrapper == Short.class) return Short.TYPE;
        if (wrapper == Character.class) return Character.TYPE;
        return null;
    }

    private boolean isWrapperClass(Class<?> cls) {
        return cls == Integer.class || cls == Long.class || cls == Float.class ||
                cls == Double.class || cls == Boolean.class || cls == Byte.class ||
                cls == Short.class || cls == Character.class;
    }

    private String successResult(Object data) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            if (data != null) {
                result.put("data", wrapResult(data));
            }
            return result.toString();
        } catch (JSONException e) {
            return "{\"success\":true,\"data\":\"" + escapeJson(String.valueOf(data)) + "\"}";
        }
    }

    private String errorResult(String message) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("error", message);
            return result.toString();
        } catch (JSONException e) {
            return "{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}";
        }
    }

    private Object wrapResult(Object obj) throws JSONException {
        if (obj == null) return JSONObject.NULL;
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean ||
                obj instanceof JSONObject || obj instanceof JSONArray) {
            return obj;
        }
        return obj.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
