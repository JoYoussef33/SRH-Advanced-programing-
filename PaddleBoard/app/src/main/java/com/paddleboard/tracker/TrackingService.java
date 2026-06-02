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
    public static final int NOTIF_ID = 1001;

    // GPS noise thresholds
    private static final float MAX_ACCURACY_M   = 20f;  // ignore fixes worse than 20 m
    private static final float MIN_DISTANCE_M   = 3f;   // ignore jumps smaller than 3 m
    private static final float MIN_SPEED_KMH    = 1.2f; // clamp to 0 below 1.2 km/h (GPS drift)

    private final IBinder binder = new LocalBinder();
    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;
    private float currentSpeedKmh = 0f;
    private float maxSpeedKmh = 0f;
    private long sessionStartTime = 0L;
    private boolean isTracking = false;

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

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    public void startTracking() {
        if (isTracking) return;
        isTracking = true;
        totalDistanceMeters = 0f;
        currentSpeedKmh = 0f;
        maxSpeedKmh = 0f;
        lastLocation = null;
        sessionStartTime = System.currentTimeMillis();

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
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f,
                locationListener, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTracking() {
        if (!isTracking) return;
        isTracking = false;
        if (locationListener != null) {
            try { locationManager.removeUpdates(locationListener); }
            catch (SecurityException ignored) {}
        }
        currentSpeedKmh = 0f;
    }

    private void handleLocation(Location loc) {
        // ── Accuracy gate ────────────────────────────────────────────────────
        // If the GPS fix is poor, report that GPS is alive but skip the update
        if (loc.getAccuracy() > MAX_ACCURACY_M) {
            if (updateListener != null) updateListener.onGpsStatusChanged(true);
            return;
        }

        // ── Distance ─────────────────────────────────────────────────────────
        if (lastLocation != null) {
            float dist = lastLocation.distanceTo(loc);
            // Only accumulate distance if the jump is real and both fixes were good
            if (dist >= MIN_DISTANCE_M && lastLocation.getAccuracy() <= MAX_ACCURACY_M) {
                totalDistanceMeters += dist;
            }
        }
        lastLocation = loc;

        // ── Speed ────────────────────────────────────────────────────────────
        float rawKmh = 0f;
        if (loc.hasSpeed()) {
            rawKmh = loc.getSpeed() * 3.6f;

        }
        // Clamp out GPS noise floor — anything below threshold is treated as still
        currentSpeedKmh = rawKmh < MIN_SPEED_KMH ? 0f : rawKmh;
        if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh;

        // ── Notify UI ────────────────────────────────────────────────────────
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (updateListener != null) {
            updateListener.onUpdate(
                totalDistanceMeters / 1000f,
                currentSpeedKmh,
                maxSpeedKmh,
                calcCalories(elapsed),
                elapsed);
            updateListener.onGpsStatusChanged(true);
        }
    }

    private int calcCalories(long elapsedMs) {
        // SUP paddling ≈ 450 kcal/hour for a 70 kg person
        return (int) (450.0 * elapsedMs / 3_600_000.0);
    }

    // ── Public accessors ──────────────────────────────────────────────────────

    public void setUpdateListener(OnTrackingUpdateListener l) { updateListener = l; }
    public boolean isTracking()        { return isTracking; }
    public float getTotalDistanceKm()  { return totalDistanceMeters / 1000f; }
    public float getCurrentSpeedKmh()  { return currentSpeedKmh; }
    public float getMaxSpeedKmh()      { return maxSpeedKmh; }
    public long  getSessionStartTime() { return sessionStartTime; }
    public int   getCalories() {
        if (!isTracking || sessionStartTime == 0) return 0;
        return calcCalories(System.currentTimeMillis() - sessionStartTime);
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }
}
