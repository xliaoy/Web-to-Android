package com.zx.keep.accounts.modules;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import org.json.JSONObject;

public class ClipboardModule {
    private final ClipboardManager clipboard;

    public ClipboardModule(Context context) {
        this.clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public String copy(String text) {
        try {
            this.clipboard.setPrimaryClip(ClipData.newPlainText("hybrid_bridge", text));
            return jsonSuccess("copied");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String paste() {
        try {
            ClipData clip = this.clipboard.getPrimaryClip();
            String text = "";
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence cs = clip.getItemAt(0).getText();
                if (cs != null) text = cs.toString();
            }
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", text);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String hasContent() {
        try {
            ClipData clip = this.clipboard.getPrimaryClip();
            boolean has = clip != null && clip.getItemCount() > 0;
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
