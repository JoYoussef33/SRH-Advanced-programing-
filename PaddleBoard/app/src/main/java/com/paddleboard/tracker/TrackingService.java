package com.paddleboard.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.location.LocationManagerCompat;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Context;
import android.os.Bundle;

public class TrackingService extends Service {

    public static final String CHANNEL_ID = "paddle_tracking_channel";
    public static final int NOTIF_ID = 1001;

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
        void onUpdate(float distanceKm, float speedKmh, float maxSpeedKmh, int calories, long elapsedMs);
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
    public IBinder onBind(Intent intent) {
        return binder;
    }

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
            @Override
            public void onLocationChanged(Location location) {
                handleLocation(location);
            }
            @Override
            public void onProviderEnabled(String provider) {
                if (updateListener != null) updateListener.onGpsStatusChanged(true);
            }
            @Override
            public void onProviderDisabled(String provider) {
                if (updateListener != null) updateListener.onGpsStatusChanged(false);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0.5f, locationListener, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTracking() {
        if (!isTracking) return;
        isTracking = false;
        if (locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) {}
        }
        currentSpeedKmh = 0f;
    }

    private void handleLocation(Location loc) {
        if (lastLocation != null) {
            float dist = lastLocation.distanceTo(loc);
            if (dist > 0.5f) {
                totalDistanceMeters += dist;
            }
        }
        lastLocation = loc;

        float rawSpeed = loc.hasSpeed() ? loc.getSpeed() : 0f;
        currentSpeedKmh = rawSpeed * 3.6f;
        if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh;

        long elapsed = System.currentTimeMillis() - sessionStartTime;
        float distKm = totalDistanceMeters / 1000f;
        int calories = calcCalories(elapsed);

        if (updateListener != null) {
            updateListener.onUpdate(distKm, currentSpeedKmh, maxSpeedKmh, calories, elapsed);
            updateListener.onGpsStatusChanged(true);
        }
    }

    private int calcCalories(long elapsedMs) {
        // SUP paddling burns ~400-500 kcal/hour for a 70kg person
        // Using 450 kcal/hour base rate
        double hours = elapsedMs / 3_600_000.0;
        return (int) (450.0 * hours);
    }

    public void setUpdateListener(OnTrackingUpdateListener listener) {
        this.updateListener = listener;
    }

    public boolean isTracking() { return isTracking; }
    public float getTotalDistanceKm() { return totalDistanceMeters / 1000f; }
    public float getCurrentSpeedKmh() { return currentSpeedKmh; }
    public float getMaxSpeedKmh() { return maxSpeedKmh; }
    public long getSessionStartTime() { return sessionStartTime; }
    public int getCalories() {
        if (!isTracking || sessionStartTime == 0) return 0;
        return calcCalories(System.currentTimeMillis() - sessionStartTime);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.channel_desc));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
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
