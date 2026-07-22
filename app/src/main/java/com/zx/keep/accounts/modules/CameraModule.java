package com.zx.keep.accounts.modules;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

public class CameraModule {
    private static CameraCallback pendingCallback;
    private static File pendingPhotoFile;
    private Activity activity;
    private final Context context;

    public interface CameraCallback {
        void onResult(JSONObject result);
    }

    public CameraModule(Context context) {
        this.context = context.getApplicationContext();
    }

    public static File getPendingPhotoFile() {
        return pendingPhotoFile;
    }

    public static void setPendingPhotoFile(File file) {
        pendingPhotoFile = file;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public String takePhoto(CameraCallback callback) {
        if (this.activity == null) {
            return jsonError("Activity not available");
        }
        try {
            pendingPhotoFile = File.createTempFile(
                    "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + "_",
                    ".jpg",
                    this.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            Uri uri = FileProvider.getUriForFile(this.context,
                    this.context.getPackageName() + ".fileprovider", pendingPhotoFile);
            pendingCallback = callback;
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra("output", uri);
            this.activity.startActivityForResult(intent, 2001);
            return jsonSuccess("camera launched");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String pickFromGallery(CameraCallback callback) {
        if (this.activity == null) {
            return jsonError("Activity not available");
        }
        pendingCallback = callback;
        this.activity.startActivityForResult(
                new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 2002);
        return jsonSuccess("gallery launched");
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (pendingCallback == null) return;
        try {
            JSONObject result = new JSONObject();
            if (resultCode != Activity.RESULT_OK) {
                result.put("success", false);
                result.put("error", "User cancelled or error occurred");
            } else if (requestCode == 2001) {
                File file = pendingPhotoFile;
                if (file != null && file.exists()) {
                    result.put("success", true);
                    result.put("type", "photo");
                    result.put("path", pendingPhotoFile.getAbsolutePath());
                } else {
                    result.put("success", false);
                    result.put("error", "Photo file not found");
                }
            } else if (requestCode == 2002) {
                if (data != null && data.getData() != null) {
                    result.put("success", true);
                    result.put("type", "gallery");
                    result.put("uri", data.getData().toString());
                } else {
                    result.put("success", false);
                    result.put("error", "No image selected");
                }
            }
            pendingCallback.onResult(result);
        } catch (Exception e) {
            try {
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("error", e.getMessage());
                pendingCallback.onResult(error);
            } catch (Exception ignored) {}
        } finally {
            pendingCallback = null;
            pendingPhotoFile = null;
        }
    }

    private String jsonSuccess(String msg) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", msg);
            return result.toString();
        } catch (Exception e) {
            return "{\"success\":true}";
        }
    }

    private String jsonError(String msg) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("error", msg);
            return result.toString();
        } catch (Exception e) {
            return "{\"success\":false}";
        }
    }
}
