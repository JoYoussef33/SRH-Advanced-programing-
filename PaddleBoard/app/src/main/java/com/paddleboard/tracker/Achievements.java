package com.paddleboard.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Badge system. Unlocks are checked when a session is saved and persist
 * in SharedPreferences. Lifetime totals are recomputed from stored sessions.
 */
public class Achievements {

    public static class Badge {
        public final String id, emoji, title, desc;
        Badge(String id, String emoji, String title, String desc) {
            this.id = id; this.emoji = emoji; this.title = title; this.desc = desc;
        }
    }

    public static final Badge[] ALL = {
        new Badge("first",      "🏄", "First Splash",   "Save your first session"),
        new Badge("km5",        "🎯", "5K Club",        "Paddle 5 km in one session"),
        new Badge("km10",       "🚀", "10K Beast",      "Paddle 10 km in one session"),
        new Badge("speed10",    "⚡", "Speed Demon",    "Hit 10 km/h"),
        new Badge("speed14",    "🐬", "Flying Fish",    "Hit 14 km/h"),
        new Badge("hour1",      "⏱", "Endurance",      "Paddle for a full hour"),
        new Badge("hour2",      "🦾", "Iron Paddler",   "Paddle for two hours straight"),
        new Badge("dawn",       "🌅", "Dawn Patrol",    "Start a session before 7 AM"),
        new Badge("dusk",       "🌇", "Sunset Rider",   "Start a session after 6 PM"),
        new Badge("cal1000",    "💪", "Furnace",        "Burn 1000 kcal in one session"),
        new Badge("total25",    "🌊", "Wave Rider",     "25 km lifetime distance"),
        new Badge("total100",   "👑", "Century Club",   "100 km lifetime distance"),
        new Badge("sessions10", "📆", "Regular",        "Save 10 sessions"),
        new Badge("sessions25", "🏆", "Devoted",        "Save 25 sessions"),
        new Badge("kcal5000",   "🔥", "Torch",          "5000 kcal lifetime burn"),
    };

    private static final String PREFS = "paddle_achievements";
    private static final String KEY   = "unlocked_v1";

    /** Lifetime totals over a list of sessions. */
    public static class Totals {
        public int   sessions;
        public float distanceKm;
        public long  durationMs;
        public int   calories;
        public float bestDistanceKm;
        public float bestSpeedKmh;
        public long  longestMs;
    }

    public static Totals totals(List<SessionData> sessions) {
        Totals t = new Totals();
        for (SessionData s : sessions) {
            t.sessions++;
            t.distanceKm += s.distanceKm;
            t.durationMs += s.durationMs;
            t.calories   += s.calories;
            if (s.distanceKm  > t.bestDistanceKm) t.bestDistanceKm = s.distanceKm;
            if (s.maxSpeedKmh > t.bestSpeedKmh)   t.bestSpeedKmh   = s.maxSpeedKmh;
            if (s.durationMs  > t.longestMs)      t.longestMs      = s.durationMs;
        }
        return t;
    }

    /**
     * Evaluate all badges against the just-saved session and lifetime totals.
     * Returns badges that unlocked right now (already-owned ones are skipped).
     */
    public static List<Badge> checkAndUnlock(Context ctx, SessionData s,
                                             List<SessionData> allIncludingNew) {
        Set<String> owned = loadUnlocked(ctx);
        Totals t = totals(allIncludingNew);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(s.startTimeMs);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        List<Badge> fresh = new ArrayList<>();
        for (Badge b : ALL) {
            if (owned.contains(b.id)) continue;
            boolean hit;
            switch (b.id) {
                case "first":      hit = t.sessions >= 1;            break;
                case "km5":        hit = s.distanceKm >= 5f;         break;
                case "km10":       hit = s.distanceKm >= 10f;        break;
                case "speed10":    hit = s.maxSpeedKmh >= 10f;       break;
                case "speed14":    hit = s.maxSpeedKmh >= 14f;       break;
                case "hour1":      hit = s.durationMs >= 3_600_000L; break;
                case "hour2":      hit = s.durationMs >= 7_200_000L; break;
                case "dawn":       hit = hour < 7;                   break;
                case "dusk":       hit = hour >= 18;                 break;
                case "cal1000":    hit = s.calories >= 1000;         break;
                case "total25":    hit = t.distanceKm >= 25f;        break;
                case "total100":   hit = t.distanceKm >= 100f;       break;
                case "sessions10": hit = t.sessions >= 10;           break;
                case "sessions25": hit = t.sessions >= 25;           break;
                case "kcal5000":   hit = t.calories >= 5000;         break;
                default:           hit = false;
            }
            if (hit) { owned.add(b.id); fresh.add(b); }
        }
        if (!fresh.isEmpty()) saveUnlocked(ctx, owned);
        return fresh;
    }

    /**
     * Which records does this session break vs. previous history?
     * Returns human-readable lines like "🏆 New best distance!".
     */
    public static List<String> newRecords(SessionData s, List<SessionData> previous) {
        Totals prev = totals(previous);
        List<String> recs = new ArrayList<>();
        if (prev.sessions == 0) return recs; // first session — everything's a record
        if (s.distanceKm  > prev.bestDistanceKm && s.distanceKm > 0.2f)
            recs.add("🏆 New best distance!");
        if (s.maxSpeedKmh > prev.bestSpeedKmh && s.maxSpeedKmh > 2f)
            recs.add("⚡ New top speed!");
        if (s.durationMs  > prev.longestMs && s.durationMs > 10 * 60_000L)
            recs.add("⏱ Longest session ever!");
        return recs;
    }

    public static Set<String> loadUnlocked(Context ctx) {
        return new HashSet<>(prefs(ctx).getStringSet(KEY, new HashSet<>()));
    }

    private static void saveUnlocked(Context ctx, Set<String> ids) {
        prefs(ctx).edit().putStringSet(KEY, ids).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
