package com.zx.keep.accounts.bridge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityManager {
    private static final Set<String> WHITELIST_PREFIXES = new HashSet<>(Arrays.asList(
            "java.lang.", "java.util.", "java.io.", "java.net.", "java.text.",
            "android.", "com.zx.keep.accounts.", "org.json."
    ));

    private static final Set<String> BLACKLIST_METHODS = new HashSet<>(Arrays.asList(
            "getClass", "getDeclaredField", "getDeclaredFields", "getField", "getFields",
            "setAccessible", "getDeclaredMethod", "getDeclaredMethods",
            "exec", "exit", "halt", "shutdown",
            "loadLibrary", "load", "getRuntime",
            "forName", "newInstance",
            "defineClass", "defineAnonymousClass",
            "addShutdownHook", "removeShutdownHook"
    ));

    public static void validate(String className, String methodName) throws SecurityException {
        boolean allowed = false;
        for (String prefix : WHITELIST_PREFIXES) {
            if (className.startsWith(prefix)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new SecurityException("Class not in whitelist: " + className);
        }

        if (methodName != null && !methodName.isEmpty()) {
            for (String blocked : BLACKLIST_METHODS) {
                if (methodName.equals(blocked)) {
                    throw new SecurityException("Method blocked: " + methodName);
                }
            }
        }
    }
}
