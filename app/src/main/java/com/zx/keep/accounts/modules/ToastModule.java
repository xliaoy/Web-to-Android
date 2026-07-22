package com.zx.keep.accounts.modules;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONObject;

public class ToastModule {
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ToastModule(Context context) {
        this.context = context.getApplicationContext();
    }

    public String show(String message) {
        showToast(message, Toast.LENGTH_SHORT);
        return successResult("shown");
    }

    public String showLong(String message) {
        showToast(message, Toast.LENGTH_LONG);
        return successResult("shown");
    }

    public String showCustom(String message, int duration) {
        showToast(message, duration);
        return successResult("shown");
    }

    private void showToast(final String message, final int duration) {
        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ToastModule.this.context, message, duration).show();
            }
        });
    }

    private String successResult(String msg) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", msg);
            return result.toString();
        } catch (Exception e) {
            return "{\"success\":true,\"data\":\"" + msg + "\"}";
        }
    }
}
