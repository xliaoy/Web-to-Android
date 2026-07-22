package com.zx.keep.accounts.modules;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import com.zx.keep.accounts.security.AuditLogger;
import org.json.JSONObject;

public class LocationModule {
    private final Context context;
    private final LocationManager locationManager;
    private LocationListener currentListener;

    public LocationModule(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public String getCurrentLocation(String provider) {
        String prov = "gps".equalsIgnoreCase(provider) ? "gps" : "network";
        try {
            if (!this.locationManager.isProviderEnabled(prov)) {
                return jsonError("Provider not enabled: " + provider);
            }
            Location location = this.locationManager.getLastKnownLocation(prov);
            if (location == null) {
                return jsonError("No last known location available");
            }
            return formatLocation(location);
        } catch (SecurityException e) {
            return jsonError("Location permission not granted");
        } catch (Exception e) {
            AuditLogger.logError("Location", e.getMessage());
            return jsonError(e.getMessage());
        }
    }

    public String requestSingleUpdate(String provider, int timeout) {
        String prov = "gps".equalsIgnoreCase(provider) ? "gps" : "network";
        try {
            if (!this.locationManager.isProviderEnabled(prov)) {
                return jsonError("Provider not enabled: " + provider);
            }
            final Location[] result = {null};
            final Object lock = new Object();
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    synchronized (lock) {
                        result[0] = location;
                        lock.notify();
                    }
                }
                @Override public void onProviderDisabled(String p) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onStatusChanged(String p, int s, Bundle b) {}
            };
            this.locationManager.requestLocationUpdates(prov, 0L, 0.0f, listener);
            synchronized (lock) {
                try { lock.wait(timeout); } catch (InterruptedException e) {}
            }
            this.locationManager.removeUpdates(listener);
            if (result[0] != null) {
                return formatLocation(result[0]);
            }
            return jsonError("Location timeout");
        } catch (SecurityException e) {
            return jsonError("Location permission not granted");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String isLocationEnabled() {
        try {
            boolean gps = this.locationManager.isProviderEnabled("gps");
            boolean network = this.locationManager.isProviderEnabled("network");
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("gps", gps);
            result.put("network", network);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    private String formatLocation(Location location) {
        try {
            JSONObject data = new JSONObject();
            data.put("latitude", location.getLatitude());
            data.put("longitude", location.getLongitude());
            data.put("accuracy", location.getAccuracy());
            data.put("altitude", location.getAltitude());
            data.put("speed", location.getSpeed());
            data.put("bearing", location.getBearing());
            data.put("provider", location.getProvider());
            data.put("timestamp", location.getTime());
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", data);
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
