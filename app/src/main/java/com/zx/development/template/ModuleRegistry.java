package com.zx.development.template;

import android.content.Context;
import android.util.Log;
import com.zx.development.template.modules.CameraModule;
import com.zx.development.template.modules.ClipboardModule;
import com.zx.development.template.modules.LocationModule;
import com.zx.development.template.modules.NetworkModule;
import com.zx.development.template.modules.SensorModule;
import com.zx.development.template.modules.StorageModule;
import com.zx.development.template.modules.ToastModule;
import com.zx.development.template.modules.VibrationModule;
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
        safeRegister("com.zx.development.template.modules.ToastModule",
                () -> new ToastModule(context));
        safeRegister("com.zx.development.template.modules.CameraModule",
                () -> new CameraModule(context));
        safeRegister("com.zx.development.template.modules.LocationModule",
                () -> new LocationModule(context));
        safeRegister("com.zx.development.template.modules.StorageModule",
                () -> new StorageModule(context));
        safeRegister("com.zx.development.template.modules.NetworkModule",
                () -> new NetworkModule(context));
        safeRegister("com.zx.development.template.modules.VibrationModule",
                () -> new VibrationModule(context));
        safeRegister("com.zx.development.template.modules.ClipboardModule",
                () -> new ClipboardModule(context));
        safeRegister("com.zx.development.template.modules.SensorModule",
                () -> new SensorModule(context));
        safeRegister("com.zx.development.template.FileAccessModule",
                FileAccessModule::new);
        safeRegister("com.zx.development.template.PermissionManager",
                () -> new PermissionManager(context));
    }
}
