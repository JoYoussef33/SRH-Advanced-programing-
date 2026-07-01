package com.paddleboard.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Fetches live paddling conditions from Open-Meteo — free, no API key.
 *   Weather:  api.open-meteo.com/v1/forecast   (wind, temp, UV, sun times)
 *   Marine:   marine-api.open-meteo.com/v1/marine (waves, water temp)
 * The last good result is cached so the card still shows offline.
 */
public class WeatherRepo {

    public interface Callback { void onResult(WeatherData data, boolean fromCache); }

    private static final String PREFS = "paddle_weather";
    private static final String KEY   = "last_weather_v1";
    /** Re-fetch no more often than this. */
    public  static final long   MAX_AGE_MS = 15 * 60_000L;

    public static void fetch(Context ctx, double lat, double lon, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());

        WeatherData cached = loadCached(ctx);
        if (cached != null &&
                System.currentTimeMillis() - cached.fetchedAtMs < MAX_AGE_MS) {
            main.post(() -> cb.onResult(cached, true));
            return;
        }

        new Thread(() -> {
            WeatherData w = new WeatherData();
            boolean gotWeather = fetchForecast(lat, lon, w);
            fetchMarine(lat, lon, w); // best-effort: NaN inland

            if (gotWeather) {
                w.fetchedAtMs = System.currentTimeMillis();
                saveCache(ctx, w);
                main.post(() -> cb.onResult(w, false));
            } else {
                // network failed — fall back to any cache, however old
                main.post(() -> cb.onResult(cached, true));
            }
        }).start();
    }

    private static boolean fetchForecast(double lat, double lon, WeatherData w) {
        try {
            String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,uv_index" +
                "&daily=sunrise,sunset&timezone=auto&forecast_days=1", lat, lon);
            JSONObject root = getJson(url);
            if (root == null) return false;

            JSONObject cur = root.getJSONObject("current");
            w.airTempC = (float) cur.optDouble("temperature_2m", Double.NaN);
            w.windKmh  = (float) cur.optDouble("wind_speed_10m", Double.NaN);
            w.gustKmh  = (float) cur.optDouble("wind_gusts_10m", Double.NaN);
            w.windDeg  = cur.optInt("wind_direction_10m", -1);
            w.uvIndex  = (float) cur.optDouble("uv_index", Double.NaN);

            JSONObject daily = root.optJSONObject("daily");
            if (daily != null) {
                w.sunrise = timeOnly(firstString(daily.optJSONArray("sunrise")));
                w.sunset  = timeOnly(firstString(daily.optJSONArray("sunset")));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void fetchMarine(double lat, double lon, WeatherData w) {
        try {
            String url = String.format(Locale.US,
                "https://marine-api.open-meteo.com/v1/marine?latitude=%.4f&longitude=%.4f" +
                "&current=wave_height,sea_surface_temperature&timezone=auto", lat, lon);
            JSONObject root = getJson(url);
            if (root == null) return;
            JSONObject cur = root.getJSONObject("current");
            w.waveHeightM = (float) cur.optDouble("wave_height", Double.NaN);
            w.waterTempC  = (float) cur.optDouble("sea_surface_temperature", Double.NaN);
        } catch (Exception ignored) { /* inland — no marine data */ }
    }

    private static JSONObject getJson(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String firstString(JSONArray a) {
        return a != null && a.length() > 0 ? a.optString(0, "") : "";
    }

    /** "2026-07-01T05:43" → "05:43" */
    private static String timeOnly(String iso) {
        int t = iso.indexOf('T');
        return t >= 0 && iso.length() >= t + 6 ? iso.substring(t + 1, t + 6) : iso;
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    public static WeatherData loadCached(Context ctx) {
        String json = prefs(ctx).getString(KEY, null);
        if (json == null) return null;
        try { return WeatherData.fromJson(new JSONObject(json)); }
        catch (JSONException e) { return null; }
    }

    private static void saveCache(Context ctx, WeatherData w) {
        try { prefs(ctx).edit().putString(KEY, w.toJson().toString()).apply(); }
        catch (JSONException ignored) {}
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
