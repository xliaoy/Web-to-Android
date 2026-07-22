package com.zx.keep.accounts.security;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuditLogger {
    private static final String TAG = "HybridAudit";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static boolean enabled = true;

    public static void setEnabled(boolean z) {
        enabled = z;
    }

    public static void logInvoke(String className, String methodName, String params) {
        if (enabled) {
            Log.i(TAG, String.format("[JSBridge] %s -- invoke: %s.%s(%s)", timestamp(), className, methodName, params));
        }
    }

    public static void logFileOp(String op, String path) {
        if (enabled) {
            Log.i(TAG, String.format("[File] %s -- %s: %s", timestamp(), op, path));
        }
    }

    public static void logPermission(String type, String permission, String result) {
        if (enabled) {
            Log.i(TAG, String.format("[Permission] %s -- %s: %s => %s", timestamp(), type, permission, result));
        }
    }

    public static void logSecurityBlock(String reason, String detail) {
        Log.w(TAG, String.format("[Security] %s -- BLOCKED: %s | %s", timestamp(), reason, detail));
    }

    public static void logError(String tag, String message) {
        Log.e(TAG, String.format("[Error] %s -- %s: %s", timestamp(), tag, message));
    }

    private static String timestamp() {
        return sdf.format(new Date());
    }
}
