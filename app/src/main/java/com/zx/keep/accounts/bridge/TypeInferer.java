package com.zx.keep.accounts.bridge;

import org.json.JSONArray;
import org.json.JSONObject;

public class TypeInferer {

    public static Object[] infer(JSONArray arr) {
        if (arr == null || arr.length() == 0) return new Object[0];
        Object[] result = new Object[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            result[i] = inferSingle(arr.opt(i));
        }
        return result;
    }

    private static Object inferSingle(Object value) {
        if (value == null || value == JSONObject.NULL) return null;
        if (value instanceof JSONObject) return value.toString();
        if (value instanceof JSONArray) return value.toString();
        if (value instanceof Boolean) return value;
        if (value instanceof Integer) return value;
        if (value instanceof Long) return value;
        if (value instanceof Double) return value;
        if (value instanceof String) return value;
        return value.toString();
    }
}
