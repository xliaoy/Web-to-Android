package com.zx.keep.accounts.modules;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import org.json.JSONObject;

public class NetworkModule {
    private final ConnectivityManager connectivityManager;

    public NetworkModule(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public String getNetworkStatus() {
        try {
            JSONObject info = new JSONObject();
            Network network = this.connectivityManager.getActiveNetwork();
            if (network == null) {
                info.put("connected", false);
                info.put("type", "none");
            } else {
                NetworkCapabilities caps = this.connectivityManager.getNetworkCapabilities(network);
                info.put("connected", true);
                if (caps != null) {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        info.put("type", "wifi");
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        info.put("type", "cellular");
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        info.put("type", "ethernet");
                    } else {
                        info.put("type", "other");
                    }
                    info.put("internet", caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
                }
            }
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", info);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String isConnected() {
        try {
            Network network = this.connectivityManager.getActiveNetwork();
            boolean connected = false;
            if (network != null) {
                NetworkCapabilities caps = this.connectivityManager.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    connected = true;
                }
            }
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", connected);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
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
