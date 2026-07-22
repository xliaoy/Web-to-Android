package com.zx.keep.accounts.bridge;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonParser {

    public static JSONObject parseObject(String json) {
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static JSONArray parseArray(String json) {
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
