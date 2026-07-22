package com.zx.keep.accounts;

import android.content.Context;
import android.util.Log;
import com.zx.keep.accounts.modules.CameraModule;
import com.zx.keep.accounts.modules.ClipboardModule;
import com.zx.keep.accounts.modules.LocationModule;
import com.zx.keep.accounts.modules.NetworkModule;
import com.zx.keep.accounts.modules.SensorModule;
import com.zx.keep.accounts.modules.StorageModule;
import com.zx.keep.accounts.modules.ToastModule;
import com.zx.keep.accounts.modules.VibrationModule;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleRegistry {
    private static final ConcurrentHashMap<String, Object> modules = new ConcurrentHashMap<>();
    private static final String TAG = "ModuleRegistry";

    public static void register(String name, Object instance) {
        modules.put(name, instance);
    }

    public static Object get(String name) {
        return modules.get(name);
    }

    private static void safeRegister(String name, ModuleFactory factory) {
        try {
            register(name, factory.create());
        } catch (Exception e) {
            Log.e(TAG, "Failed to register module: " + name, e);
        }
    }

    private interface ModuleFactory {
        Object create() throws Exception;
    }

    public static void registerAll(Context context) {
        safeRegister("com.zx.keep.accounts.modules.ToastModule",
                () -> new ToastModule(context));
        safeRegister("com.zx.keep.accounts.modules.CameraModule",
                () -> new CameraModule(context));
        safeRegister("com.zx.keep.accounts.modules.LocationModule",
                () -> new LocationModule(context));
        safeRegister("com.zx.keep.accounts.modules.StorageModule",
                () -> new StorageModule(context));
        safeRegister("com.zx.keep.accounts.modules.NetworkModule",
                () -> new NetworkModule(context));
        safeRegister("com.zx.keep.accounts.modules.VibrationModule",
                () -> new VibrationModule(context));
        safeRegister("com.zx.keep.accounts.modules.ClipboardModule",
                () -> new ClipboardModule(context));
        safeRegister("com.zx.keep.accounts.modules.SensorModule",
                () -> new SensorModule(context));
        safeRegister("com.zx.keep.accounts.FileAccessModule",
                FileAccessModule::new);
        safeRegister("com.zx.keep.accounts.PermissionManager",
                () -> new PermissionManager(context));
    }
}
