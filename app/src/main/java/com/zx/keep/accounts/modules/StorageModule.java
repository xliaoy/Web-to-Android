package com.zx.keep.accounts.modules;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public class StorageModule {
    private final SharedPreferences prefs;

    public StorageModule(Context context) {
        this.prefs = context.getSharedPreferences("HybridBridgeStorage", Context.MODE_PRIVATE);
    }

    public String setString(String key, String value) {
        this.prefs.edit().putString(key, value).apply();
        return jsonSuccess("saved");
    }

    public String getString(String key) {
        Object value = this.prefs.getString(key, null);
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", value == null ? JSONObject.NULL : value);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String setInt(String key, int value) {
        this.prefs.edit().putInt(key, value).apply();
        return jsonSuccess("saved");
    }

    public String getInt(String key, int defaultValue) {
        int value = this.prefs.getInt(key, defaultValue);
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", value);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String setBoolean(String key, boolean value) {
        this.prefs.edit().putBoolean(key, value).apply();
        return jsonSuccess("saved");
    }

    public String getBoolean(String key, boolean defaultValue) {
        boolean value = this.prefs.getBoolean(key, defaultValue);
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", value);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String remove(String key) {
        this.prefs.edit().remove(key).apply();
        return jsonSuccess("removed");
    }

    public String clear() {
        this.prefs.edit().clear().apply();
        return jsonSuccess("cleared");
    }

    public String contains(String key) {
        boolean exists = this.prefs.contains(key);
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", exists);
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
