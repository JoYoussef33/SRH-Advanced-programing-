package com.paddleboard.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SessionStorage {
    private static final String PREFS = "paddle_sessions";
    private static final String KEY   = "sessions_v1";

    public static void save(Context ctx, SessionData s) {
        SharedPreferences p = prefs(ctx);
        JSONArray arr = raw(p);
        try { arr.put(s.toJson()); } catch (JSONException e) { return; }
        p.edit().putString(KEY, arr.toString()).apply();
    }

    public static List<SessionData> loadAll(Context ctx) {
        JSONArray arr = raw(prefs(ctx));
        List<SessionData> list = new ArrayList<>();
        for (int i = arr.length() - 1; i >= 0; i--) {
            try { list.add(SessionData.fromJson(arr.getJSONObject(i))); }
            catch (JSONException ignored) {}
        }
        return list;
    }

    public static void delete(Context ctx, long id) {
        SharedPreferences p = prefs(ctx);
        JSONArray arr = raw(p), out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                if (o.getLong("id") != id) out.put(o);
            } catch (JSONException ignored) {}
        }
        p.edit().putString(KEY, out.toString()).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONArray raw(SharedPreferences p) {
        try { return new JSONArray(p.getString(KEY, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
}
