package com.zx.keep.accounts.modules;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import org.json.JSONObject;

public class VibrationModule {
    private final Vibrator vibrator;

    public VibrationModule(Context context) {
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public String vibrateShort() {
        return vibrate(100);
    }

    public String vibrateLong() {
        return vibrate(500);
    }

    public String vibrate(int duration) {
        if (this.vibrator == null || !this.vibrator.hasVibrator()) {
            return jsonError("Vibrator not available");
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                this.vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                this.vibrator.vibrate(duration);
            }
            return jsonSuccess("vibrated");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String vibratePattern(long[] pattern, int repeat) {
        if (this.vibrator == null || !this.vibrator.hasVibrator()) {
            return jsonError("Vibrator not available");
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                this.vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
            } else {
                this.vibrator.vibrate(pattern, repeat);
            }
            return jsonSuccess("vibrated");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String cancel() {
        if (this.vibrator != null) {
            this.vibrator.cancel();
        }
        return jsonSuccess("cancelled");
    }

    public String hasVibrator() {
        boolean has = this.vibrator != null && this.vibrator.hasVibrator();
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", has);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
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
