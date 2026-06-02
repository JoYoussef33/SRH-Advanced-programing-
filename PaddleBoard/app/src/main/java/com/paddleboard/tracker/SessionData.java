package com.paddleboard.tracker;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionData {
    public long  id;
    public long  startTimeMs;
    public long  endTimeMs;
    public long  durationMs;
    public float distanceKm;
    public float maxSpeedKmh;
    public float avgSpeedKmh;
    public int   calories;

    public String formatDate() {
        return new SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                .format(new Date(startTimeMs));
    }

    public String formatEndTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(endTimeMs));
    }

    public String formatDuration() {
        long s = durationMs / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    // 0 = sarcastic, 1 = meh, 2 = good, 3 = legendary
    public int tier() {
        int score = 0;
        float mins = durationMs / 60000f;
        if      (distanceKm >= 8f)   score += 4;
        else if (distanceKm >= 3f)   score += 3;
        else if (distanceKm >= 1f)   score += 2;
        else if (distanceKm >= 0.2f) score += 1;

        if      (mins >= 60) score += 4;
        else if (mins >= 30) score += 3;
        else if (mins >= 15) score += 2;
        else if (mins >= 5)  score += 1;

        if      (maxSpeedKmh >= 8f) score += 2;
        else if (maxSpeedKmh >= 4f) score += 1;

        if      (score >= 8) return 3;
        else if (score >= 5) return 2;
        else if (score >= 2) return 1;
        else                 return 0;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id",          id);
        o.put("startTimeMs", startTimeMs);
        o.put("endTimeMs",   endTimeMs);
        o.put("durationMs",  durationMs);
        o.put("distanceKm",  distanceKm);
        o.put("maxSpeedKmh", maxSpeedKmh);
        o.put("avgSpeedKmh", avgSpeedKmh);
        o.put("calories",    calories);
        return o;
    }

    public static SessionData fromJson(JSONObject o) throws JSONException {
        SessionData s   = new SessionData();
        s.id            = o.getLong("id");
        s.startTimeMs   = o.getLong("startTimeMs");
        s.endTimeMs     = o.getLong("endTimeMs");
        s.durationMs    = o.getLong("durationMs");
        s.distanceKm    = (float) o.getDouble("distanceKm");
        s.maxSpeedKmh   = (float) o.getDouble("maxSpeedKmh");
        s.avgSpeedKmh   = (float) o.getDouble("avgSpeedKmh");
        s.calories      = o.getInt("calories");
        return s;
    }
}
