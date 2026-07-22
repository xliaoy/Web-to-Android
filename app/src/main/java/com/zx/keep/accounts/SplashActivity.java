package com.zx.keep.accounts;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.zx.keep.accounts.security.AuditLogger;
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 1002;
    private static final String MANAGE_PERM = "android.permission.MANAGE_EXTERNAL_STORAGE";
    private boolean permissionFlowComplete = false;
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0x00000000);
        setContentView(new android.view.View(this));

        this.permissionManager = new PermissionManager(this);
        this.permissionManager.setActivity(this);
        new Handler(Looper.getMainLooper()).post(this::startPermissionFlow);
    }

    private void startPermissionFlow() {
        List<String> ungranted = this.permissionManager.getUngrantedPermissions();
        if (ungranted.isEmpty() && hasManageStoragePermission()) {
            proceedToMain();
            return;
        }

        ArrayList<String> normalPerms = new ArrayList<>();
        boolean needManageStorage = false;

        for (String perm : ungranted) {
            if (MANAGE_PERM.equals(perm)) {
                needManageStorage = true;
            } else {
                normalPerms.add(perm);
            }
        }

        if (!needManageStorage && Build.VERSION.SDK_INT >= 30) {
            needManageStorage = !Environment.isExternalStorageManager();
        }

        if (!normalPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    normalPerms.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else if (needManageStorage) {
            openManageStorageSettings();
        } else {
            proceedToMain();
        }
    }

    private boolean hasManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                AuditLogger.logError("Splash", "manageStorage: " + e.getMessage());
                proceedToMain();
            }
        } else {
            proceedToMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_PERMISSIONS) return;

        for (int i = 0; i < permissions.length; i++) {
            AuditLogger.logPermission("splash_request", permissions[i],
                    grantResults[i] == 0 ? "granted" : "denied");
        }

        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                String[] declared = getPackageManager()
                        .getPackageInfo(getPackageName(), 4096).requestedPermissions;
                if (declared != null) {
                    for (String p : declared) {
                        if (MANAGE_PERM.equals(p)) {
                            openManageStorageSettings();
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        proceedToMain();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            proceedToMain();
        }
    }

    private void proceedToMain() {
        if (this.permissionFlowComplete) return;
        this.permissionFlowComplete = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
