package com.paddleboard.tracker;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Snapshot of paddling conditions from Open-Meteo (weather + marine).
 * Marine fields (waterTempC, waveHeightM) are Float.NaN when unavailable
 * (e.g. inland lakes not covered by the marine model).
 */
public class WeatherData {
    public float  airTempC     = Float.NaN;
    public float  windKmh      = Float.NaN;
    public float  gustKmh      = Float.NaN;
    public int    windDeg      = -1;
    public float  uvIndex      = Float.NaN;
    public String sunrise      = "";
    public String sunset       = "";
    public float  waterTempC   = Float.NaN;
    public float  waveHeightM  = Float.NaN;
    public long   fetchedAtMs  = 0L;

    /**
     * Paddle Score 0–100: how good conditions are for SUP right now.
     * Penalties are based on published SUP safety guidance:
     * beginners should avoid wind above ~12 knots (~22 km/h) and
     * flat-water paddling is best under 0.5 m waves.
     */
    public int score() {
        float s = 100f;

        if (!Float.isNaN(windKmh) && windKmh > 8f)  s -= (windKmh - 8f) * 2.2f;
        if (!Float.isNaN(gustKmh) && gustKmh > 20f) s -= (gustKmh - 20f) * 1.2f;
        if (!Float.isNaN(waveHeightM) && waveHeightM > 0.3f)
            s -= (waveHeightM - 0.3f) * 45f;
        if (!Float.isNaN(airTempC)) {
            if (airTempC < 18f) s -= (18f - airTempC) * 2f;
            if (airTempC > 33f) s -= (airTempC - 33f) * 2f;
        }
        if (!Float.isNaN(uvIndex) && uvIndex > 8f) s -= (uvIndex - 8f) * 2f;

        return Math.max(0, Math.min(100, Math.round(s)));
    }

    public String verdict() {
        int s = score();
        if (s >= 80) return "Perfect day to paddle! 🏄";
        if (s >= 60) return "Good conditions — go get it 🌊";
        if (s >= 40) return "Doable, but respect the wind 💨";
        if (s >= 20) return "Choppy — stay close to shore ⚠️";
        return "The ocean says not today 🛋️";
    }

    public int scoreColor() {
        int s = score();
        if (s >= 80) return 0xFF00FFD4; // teal
        if (s >= 60) return 0xFF00D4FF; // cyan
        if (s >= 40) return 0xFFFFD166; // yellow
        return 0xFFFF6B35;              // coral
    }

    /** Compass direction the wind is coming FROM ("NW" etc.). */
    public String windCompass() {
        if (windDeg < 0) return "";
        String[] dirs = {"N","NE","E","SE","S","SW","W","NW"};
        return dirs[Math.round(windDeg / 45f) % 8];
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("airTempC", airTempC);   o.put("windKmh", windKmh);
        o.put("gustKmh", gustKmh);     o.put("windDeg", windDeg);
        o.put("uvIndex", uvIndex);     o.put("sunrise", sunrise);
        o.put("sunset", sunset);       o.put("waterTempC", waterTempC);
        o.put("waveHeightM", waveHeightM);
        o.put("fetchedAtMs", fetchedAtMs);
        return o;
    }

    public static WeatherData fromJson(JSONObject o) {
        WeatherData w = new WeatherData();
        w.airTempC    = (float) o.optDouble("airTempC", Double.NaN);
        w.windKmh     = (float) o.optDouble("windKmh", Double.NaN);
        w.gustKmh     = (float) o.optDouble("gustKmh", Double.NaN);
        w.windDeg     = o.optInt("windDeg", -1);
        w.uvIndex     = (float) o.optDouble("uvIndex", Double.NaN);
        w.sunrise     = o.optString("sunrise", "");
        w.sunset      = o.optString("sunset", "");
        w.waterTempC  = (float) o.optDouble("waterTempC", Double.NaN);
        w.waveHeightM = (float) o.optDouble("waveHeightM", Double.NaN);
        w.fetchedAtMs = o.optLong("fetchedAtMs", 0L);
        return w;
    }
}
