package com.zx.keep.accounts.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityConfig {
    public static final Set<String> CLASS_WHITELIST = new HashSet<>(Arrays.asList(
            "com.zx.keep.accounts.modules.ToastModule",
            "com.zx.keep.accounts.modules.CameraModule",
            "com.zx.keep.accounts.modules.LocationModule",
            "com.zx.keep.accounts.modules.StorageModule",
            "com.zx.keep.accounts.modules.NetworkModule",
            "com.zx.keep.accounts.modules.VibrationModule",
            "com.zx.keep.accounts.modules.ClipboardModule",
            "com.zx.keep.accounts.modules.SensorModule",
            "com.zx.keep.accounts.FileAccessModule",
            "com.zx.keep.accounts.PermissionManager",
            "java.lang.System", "java.lang.String", "java.lang.Math",
            "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.lang.Boolean", "java.lang.Byte",
            "java.lang.Short", "java.lang.Character", "java.util.UUID",
            "java.util.Date", "java.text.SimpleDateFormat", "java.util.Locale",
            "java.util.TimeZone", "java.util.Calendar",
            "org.json.JSONObject", "org.json.JSONArray", "java.io.File"
    ));

    public static final Set<String> METHOD_BLACKLIST = new HashSet<>(Arrays.asList(
            "forName", "getDeclaredMethod", "getDeclaredField", "getMethod",
            "getField", "getClass", "newInstance", "exec", "destroy", "exit",
            "halt", "load", "loadLibrary", "defineClass", "findClass",
            "findSystemClass", "setSecurityManager", "getSecurityManager",
            "readObject", "writeObject", "start", "command", "directory",
            "environment", "redirectErrorStream"
    ));

    public static final Set<String> CLASS_BLACKLIST = new HashSet<>(Arrays.asList(
            "java.lang.Class", "java.lang.ClassLoader", "java.lang.Runtime",
            "java.lang.Process", "java.lang.ProcessBuilder", "java.lang.Thread",
            "java.lang.ThreadGroup", "java.lang.reflect.AccessibleObject",
            "java.lang.reflect.Field", "java.lang.reflect.Method",
            "java.lang.reflect.Constructor", "java.lang.reflect.Modifier",
            "java.security.AccessController", "java.lang.invoke.MethodHandles"
    ));

    public static boolean isClassAllowed(String className) {
        if (CLASS_BLACKLIST.contains(className)) {
            return false;
        }
        return CLASS_WHITELIST.contains(className);
    }

    public static boolean isMethodBlocked(String methodName) {
        return METHOD_BLACKLIST.contains(methodName);
    }
}
