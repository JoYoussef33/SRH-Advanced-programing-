package com.paddleboard.tracker;

import android.content.Context;
import android.content.SharedPreferences;

/** User settings — currently body weight, used by the MET calorie model. */
public class UserProfile {
    private static final String PREFS = "paddle_profile";
    private static final String KEY_WEIGHT = "weight_kg";
    public  static final float  DEFAULT_WEIGHT_KG = 75f;

    public static float getWeightKg(Context ctx) {
        return prefs(ctx).getFloat(KEY_WEIGHT, DEFAULT_WEIGHT_KG);
    }

    public static void setWeightKg(Context ctx, float kg) {
        prefs(ctx).edit().putFloat(KEY_WEIGHT, kg).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
