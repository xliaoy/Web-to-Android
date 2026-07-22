package com.zx.keep.accounts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.zx.keep.accounts.security.AuditLogger;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class PermissionManager {
    private static PermissionCallback pendingCallback;
    private Activity activity;
    private final Context context;

    public interface PermissionCallback {
        void onResult(JSONObject result);
    }

    public PermissionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public JSONArray getDeclaredDangerousPermissions() {
        JSONArray jsonArray = new JSONArray();
        try {
            String[] permissions = this.context.getPackageManager()
                    .getPackageInfo(this.context.getPackageName(), 4096).requestedPermissions;
            if (permissions != null) {
                for (String perm : permissions) {
                    if (isDangerousPermission(perm)) {
                        jsonArray.put(perm);
                    }
                }
            }
        } catch (Exception e) {
            AuditLogger.logError("Permission", "getDeclared: " + e.getMessage());
        }
        return jsonArray;
    }

    public List<String> getUngrantedPermissions() {
        ArrayList<String> list = new ArrayList<>();
        try {
            String[] permissions = this.context.getPackageManager()
                    .getPackageInfo(this.context.getPackageName(), 4096).requestedPermissions;
            if (permissions != null) {
                for (String perm : permissions) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if ("android.permission.READ_EXTERNAL_STORAGE".equals(perm) ||
                                "android.permission.WRITE_EXTERNAL_STORAGE".equals(perm)) {
                            continue;
                        }
                    }
                    if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(perm)) {
                        continue;
                    }
                    if (isDangerousPermission(perm) &&
                            ContextCompat.checkSelfPermission(this.context, perm) != 0) {
                        list.add(perm);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public String checkPermissionStatus(String permission) {
        if (Build.VERSION.SDK_INT >= 30 &&
                "android.permission.MANAGE_EXTERNAL_STORAGE".equals(permission)) {
            return Environment.isExternalStorageManager() ? "granted" : "denied";
        }
        if (ContextCompat.checkSelfPermission(this.context, permission) == 0) {
            return "granted";
        }
        Activity act = this.activity;
        return (act == null || !ActivityCompat.shouldShowRequestPermissionRationale(act, permission))
                ? "never_ask" : "denied";
    }

    public void requestPermissions(String[] permissions, PermissionCallback callback) {
        if (this.activity == null) {
            AuditLogger.logError("Permission", "Activity not set");
            if (callback != null) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("success", false);
                    result.put("error", "Activity not available");
                    callback.onResult(result);
                } catch (Exception ignored) {
                }
            }
            return;
        }
        pendingCallback = callback;
        ArrayList<String> needRequest = new ArrayList<>();
        JSONObject result = new JSONObject();
        try {
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this.context, perm) == 0) {
                    result.put(perm, "granted");
                } else {
                    needRequest.add(perm);
                }
            }
        } catch (Exception e) {
            AuditLogger.logError("Permission", "requestPermissions: " + e.getMessage());
        }
        if (!needRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this.activity,
                    needRequest.toArray(new String[0]), 1001);
        } else if (callback != null) {
            try {
                result.put("success", true);
                callback.onResult(result);
            } catch (Exception ignored) {
            }
        }
    }

    public void openAppSettings() {
        if (this.activity == null) return;
        try {
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.parse("package:" + this.context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.activity.startActivity(intent);
        } catch (Exception e) {
            AuditLogger.logError("Permission", "openSettings: " + e.getMessage());
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != 1001 || pendingCallback == null) return;
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            for (int i = 0; i < permissions.length; i++) {
                String status = grantResults[i] == 0 ? "granted" : "denied";
                result.put(permissions[i], status);
                AuditLogger.logPermission("request", permissions[i], status);
            }
            pendingCallback.onResult(result);
        } catch (Exception e) {
            AuditLogger.logError("Permission", "onResult: " + e.getMessage());
        } finally {
            pendingCallback = null;
        }
    }

    private boolean isDangerousPermission(String perm) {
        if (perm == null) return false;
        return perm.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                perm.equals("android.permission.WRITE_EXTERNAL_STORAGE") ||
                perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE");
    }
}
