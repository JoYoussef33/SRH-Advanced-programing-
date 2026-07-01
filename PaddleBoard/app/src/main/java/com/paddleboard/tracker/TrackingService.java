package com.paddleboard.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class TrackingService extends Service {

    public static final String CHANNEL_ID = "paddle_tracking_channel";
    public static final int    NOTIF_ID   = 1001;

    // ── GPS Quality thresholds ────────────────────────────────────────────────
    /** Reject fixes worse than this horizontal accuracy (meters). */
    private static final float MAX_ACCURACY_M  = 18f;
    /** Minimum distance between points to count as real movement. */
    private static final float MIN_DIST_M      = 2.5f;
    /** Speed below this is treated as stationary (GPS noise floor). */
    private static final float MIN_SPEED_KMH   = 1.0f;
    /**
     * Max plausible SUP speed (km/h). Location jumps implying faster than
     * this are GPS glitches and are discarded.
     */
    private static final float MAX_PLAUSIBLE_KMH = 28f;
    /**
     * Exponential moving-average factor for speed smoothing.
     * Lower = smoother but more lag; higher = more responsive but noisier.
     */
    private static final float SPEED_ALPHA = 0.25f;

    private final IBinder binder = new LocalBinder();
    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location lastGoodLocation     = null;
    private float    totalDistanceMeters  = 0f;
    private float    rawSpeedKmh          = 0f;
    private float    smoothedSpeedKmh     = 0f; // EMA-filtered speed
    private float    maxSpeedKmh          = 0f;
    private long     sessionStartTime     = 0L;
    private boolean  isTracking           = false;
    private float    userWeightKg         = UserProfile.DEFAULT_WEIGHT_KG;

    private OnTrackingUpdateListener updateListener;

    public interface OnTrackingUpdateListener {
        void onUpdate(float distanceKm, float speedKmh, float maxSpeedKmh,
                      int calories, long elapsedMs);
        void onGpsStatusChanged(boolean hasGps);
    }

    public class LocalBinder extends Binder {
        TrackingService getService() { return TrackingService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    public void startTracking() {
        if (isTracking) return;
        isTracking            = true;
        totalDistanceMeters   = 0f;
        rawSpeedKmh           = 0f;
        smoothedSpeedKmh      = 0f;
        maxSpeedKmh           = 0f;
        lastGoodLocation      = null;
        sessionStartTime      = System.currentTimeMillis();
        userWeightKg          = UserProfile.getWeightKg(this);

        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) { handleLocation(loc); }
            @Override public void onProviderEnabled(String p) {
                if (updateListener != null) updateListener.onGpsStatusChanged(true);
            }
            @Override public void onProviderDisabled(String p) {
                if (updateListener != null) updateListener.onGpsStatusChanged(false);
            }
            @Override public void onStatusChanged(String p, int s, Bundle e) {}
        };

        try {
            // Request updates as fast as possible; accuracy filter in handleLocation
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 500L, 0f,
                locationListener, Looper.getMainLooper());
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    public void stopTracking() {
        if (!isTracking) return;
        isTracking = false;
        if (locationListener != null) {
            try { locationManager.removeUpdates(locationListener); }
            catch (SecurityException ignored) {}
        }
        smoothedSpeedKmh = 0f;
        rawSpeedKmh      = 0f;
    }

    private void handleLocation(Location loc) {
        // ── 1. Accuracy gate ─────────────────────────────────────────────────
        if (loc.getAccuracy() > MAX_ACCURACY_M) {
            if (updateListener != null) updateListener.onGpsStatusChanged(true);
            return;
        }

        // ── 2. GPS jump rejection ────────────────────────────────────────────
        if (lastGoodLocation != null) {
            float dist      = lastGoodLocation.distanceTo(loc);
            long  dtMs      = loc.getTime() - lastGoodLocation.getTime();

            // If the implied speed of the jump exceeds the physical maximum, skip it
            if (dtMs > 0) {
                float impliedKmh = (dist / (dtMs / 1000f)) * 3.6f;
                if (impliedKmh > MAX_PLAUSIBLE_KMH + 5f) {
                    // This is a GPS glitch — update last location so we don't
                    // keep rejecting everything if GPS has drifted
                    lastGoodLocation = loc;
                    return;
                }
            }

            // Only count distance when both fixes have good accuracy
            // and the step is meaningfully larger than GPS noise
            if (dist >= MIN_DIST_M && lastGoodLocation.getAccuracy() <= MAX_ACCURACY_M) {
                totalDistanceMeters += dist;
            }
        }
        lastGoodLocation = loc;

        // ── 3. Speed: read, check, smooth ────────────────────────────────────
        float newRawKmh = 0f;
        if (loc.hasSpeed()) {
            newRawKmh = loc.getSpeed() * 3.6f;
            // Clamp to plausible range before smoothing
            if (newRawKmh > MAX_PLAUSIBLE_KMH) newRawKmh = MAX_PLAUSIBLE_KMH;
        }
        rawSpeedKmh = newRawKmh;

        // Exponential moving average smooths out short GPS speed spikes
        smoothedSpeedKmh = SPEED_ALPHA * rawSpeedKmh + (1f - SPEED_ALPHA) * smoothedSpeedKmh;

        // Hard noise floor — below MIN_SPEED_KMH we report 0
        float displaySpeed = smoothedSpeedKmh < MIN_SPEED_KMH ? 0f : smoothedSpeedKmh;

        if (displaySpeed > maxSpeedKmh) maxSpeedKmh = displaySpeed;

        // ── 4. Notify UI ──────────────────────────────────────────────────────
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (updateListener != null) {
            updateListener.onUpdate(
                totalDistanceMeters / 1000f,
                displaySpeed,
                maxSpeedKmh,
                calcCalories(elapsed),
                elapsed);
            updateListener.onGpsStatusChanged(true);
        }
    }

    // ── SUP calorie model (MET-based) ─────────────────────────────────────────
    // kcal = MET × weight(kg) × hours, with MET values from the Compendium
    // of Physical Activities: SUP leisurely ≈ 4.5, general ≈ 6.0,
    // vigorous effort ≈ 7.8, racing ≈ 12.5. MET is picked from average pace.
    private int calcCalories(long elapsedMs) {
        double hours = elapsedMs / 3_600_000.0;
        if (hours <= 0) return 0;
        float avgKmh = (totalDistanceMeters / 1000f) / (float) hours;

        float met;
        if      (avgKmh < 3.5f) met = 4.5f;  // leisurely drift
        else if (avgKmh < 6.0f) met = 6.0f;  // steady cruising
        else if (avgKmh < 8.0f) met = 7.8f;  // vigorous effort
        else                    met = 12.5f; // race pace

        return (int) (met * userWeightKg * hours);
    }

    // ── Public accessors ───────────────────────────────────────────────────────
    public void setUpdateListener(OnTrackingUpdateListener l) { updateListener = l; }
    public boolean isTracking()        { return isTracking; }
    public float getTotalDistanceKm()  { return totalDistanceMeters / 1000f; }
    public float getCurrentSpeedKmh()  { return smoothedSpeedKmh < MIN_SPEED_KMH ? 0f : smoothedSpeedKmh; }
    public float getMaxSpeedKmh()      { return maxSpeedKmh; }
    public long  getSessionStartTime() { return sessionStartTime; }
    public int   getCalories() {
        if (!isTracking || sessionStartTime == 0) return 0;
        return calcCalories(System.currentTimeMillis() - sessionStartTime);
    }

    // ── Notification ───────────────────────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override public void onDestroy() { stopTracking(); super.onDestroy(); }
}
