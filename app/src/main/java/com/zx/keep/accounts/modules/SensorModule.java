package com.zx.keep.accounts.modules;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class SensorModule implements SensorEventListener {
    private final Context context;
    private final SensorManager sensorManager;
    private float[] lastAccelerometer = new float[3];
    private float[] lastGyroscope = new float[3];
    private float[] lastMagnetic = new float[3];
    private float lastLight = 0.0f;
    private float lastProximity = 0.0f;
    private float lastPressure = 0.0f;

    public SensorModule(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public String getAccelerometer() {
        return getSensorData("accelerometer", this.lastAccelerometer);
    }

    public String getGyroscope() {
        return getSensorData("gyroscope", this.lastGyroscope);
    }

    public String getMagneticField() {
        return getSensorData("magnetic", this.lastMagnetic);
    }

    public String getLight() {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "light");
            data.put("value", this.lastLight);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", data);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String getProximity() {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "proximity");
            data.put("value", this.lastProximity);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", data);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String getPressure() {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "pressure");
            data.put("value", this.lastPressure);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", data);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String listSensors() {
        try {
            JSONArray list = new JSONArray();
            for (Sensor s : this.sensorManager.getSensorList(Sensor.TYPE_ALL)) {
                JSONObject item = new JSONObject();
                item.put("name", s.getName());
                item.put("type", s.getType());
                item.put("vendor", s.getVendor());
                item.put("version", s.getVersion());
                item.put("power", s.getPower());
                item.put("maxRange", s.getMaximumRange());
                item.put("resolution", s.getResolution());
                list.put(item);
            }
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", list);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String startListening() {
        try {
            Sensor accel = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel != null) this.sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
            Sensor gyro = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyro != null) this.sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
            Sensor mag = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (mag != null) this.sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI);
            Sensor light = this.sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (light != null) this.sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
            Sensor prox = this.sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (prox != null) this.sensorManager.registerListener(this, prox, SensorManager.SENSOR_DELAY_UI);
            Sensor pressure = this.sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            if (pressure != null) this.sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_UI);
            return jsonSuccess("listening started");
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String stopListening() {
        this.sensorManager.unregisterListener(this);
        return jsonSuccess("listening stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, this.lastAccelerometer, 0, event.values.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, this.lastMagnetic, 0, event.values.length);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, this.lastGyroscope, 0, event.values.length);
                break;
            case Sensor.TYPE_LIGHT:
                this.lastLight = event.values[0];
                break;
            case Sensor.TYPE_PRESSURE:
                this.lastPressure = event.values[0];
                break;
            case Sensor.TYPE_PROXIMITY:
                this.lastProximity = event.values[0];
                break;
        }
    }

    private String getSensorData(String type, float[] values) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", type);
            JSONArray arr = new JSONArray();
            for (float v : values) arr.put(v);
            data.put("values", arr);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", data);
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
