package com.paddleboard.tracker;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.paddleboard.tracker.databinding.ActivityMainBinding;
import org.json.JSONException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements TrackingService.OnTrackingUpdateListener {

    private ActivityMainBinding b;
    private TrackingService trackingService;
    private boolean serviceBound = false;
    private boolean sessionActive = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private AnimatorSet breatheAnim;

    private final ActivityResultLauncher<String> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startSession();
            else Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_LONG).show();
        });

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder binder) {
            trackingService = ((TrackingService.LocalBinder) binder).getService();
            trackingService.setUpdateListener(MainActivity.this);
            serviceBound = true;
            if (trackingService.isTracking()) {
                sessionActive = true;
                applyActiveState();
                startTimerTick();
            }
        }
        @Override public void onServiceDisconnected(ComponentName n) { serviceBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        applyIdleState();

        b.btnStartStop.setOnClickListener(v -> {
            squishButton();
            if (sessionActive) stopSession();
            else checkPermission();
        });

        b.btnHistory.setOnClickListener(v ->
            startActivity(new Intent(this, HistoryActivity.class)));

        bindService(new Intent(this, TrackingService.class), conn, Context.BIND_AUTO_CREATE);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) startSession();
        else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void startSession() {
        ContextCompat.startForegroundService(this, new Intent(this, TrackingService.class));
        if (serviceBound) {
            trackingService.startTracking();
            sessionActive = true;
            applyActiveState();
            startTimerTick();
            animateCardsIn();
            b.tvStatus.setText("Session active — paddle hard! 🌊");
        }
    }

    private void stopSession() {
        if (!serviceBound) return;

        // Snapshot final stats before stopping
        long startTime = trackingService.getSessionStartTime();
        long endTime   = System.currentTimeMillis();
        long duration  = endTime - startTime;
        float distKm   = trackingService.getTotalDistanceKm();
        float maxSpeed = trackingService.getMaxSpeedKmh();
        float avgSpeed = duration > 0 ? distKm / (duration / 3_600_000f) : 0f;
        int  calories  = trackingService.getCalories();

        trackingService.stopTracking();
        sessionActive = false;
        timerHandler.removeCallbacksAndMessages(null);
        applyIdleState();
        b.tvStatus.setText("Ready to paddle 🌊");

        // Build session and launch result screen
        SessionData s = new SessionData();
        s.id          = startTime;
        s.startTimeMs = startTime;
        s.endTimeMs   = endTime;
        s.durationMs  = duration;
        s.distanceKm  = distKm;
        s.maxSpeedKmh = maxSpeed;
        s.avgSpeedKmh = avgSpeed;
        s.calories    = calories;

        try {
            Intent i = new Intent(this, SessionEndActivity.class);
            i.putExtra(SessionEndActivity.EXTRA_SESSION, s.toJson().toString());
            startActivity(i);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
        } catch (JSONException e) {
            Toast.makeText(this, "Session data error", Toast.LENGTH_SHORT).show();
        }

        // Reset displayed stats
        b.tvDistance.setText("0.00");
        b.tvSpeed.setText("0.0");
        b.tvMaxSpeed.setText("0.0");
        b.tvCalories.setText("0");
        b.tvDuration.setText("00:00:00");
    }

    // ── Button visual states ──────────────────────────────────────────────────

    private void applyIdleState() {
        b.btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_circle_cyan));
        b.btnStartStop.setText("START\nSESSION");
        b.btnStartStop.setTextColor(getColor(R.color.ocean_deep));
        b.waterRipple.setRippleColor(0xFF00D4FF); // cyan
        b.waterRipple.setActive(false);
        stopBreath(); startBreath(0.93f, 1.07f, 1700);
    }

    private void applyActiveState() {
        b.btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_circle_coral));
        b.btnStartStop.setText("STOP\nSESSION");
        b.btnStartStop.setTextColor(Color.WHITE);
        b.waterRipple.setRippleColor(0xFFFF6B35); // coral
        b.waterRipple.setActive(true);
        stopBreath(); startBreath(0.96f, 1.04f, 900);
    }

    private void startBreath(float from, float to, long dur) {
        breatheAnim = new AnimatorSet();
        breatheAnim.playTogether(
            breathe(b.btnStartStop, "scaleX", from, to, dur),
            breathe(b.btnStartStop, "scaleY", from, to, dur));
        breatheAnim.start();
    }

    private void stopBreath() {
        if (breatheAnim != null) { breatheAnim.cancel(); breatheAnim = null; }
        b.btnStartStop.setScaleX(1f); b.btnStartStop.setScaleY(1f);
    }

    private ObjectAnimator breathe(android.view.View v, String prop, float from, float to, long dur) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur); a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        return a;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void startTimerTick() {
        timerHandler.post(new Runnable() {
            @Override public void run() {
                if (!sessionActive || !serviceBound) return;
                long elapsed = System.currentTimeMillis() - trackingService.getSessionStartTime();
                b.tvDuration.setText(formatDuration(elapsed));
                b.tvCalories.setText(String.valueOf(trackingService.getCalories()));
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    // ── Tracking callbacks ────────────────────────────────────────────────────

    @Override
    public void onUpdate(float distKm, float speedKmh, float maxSpeedKmh, int calories, long elapsedMs) {
        runOnUiThread(() -> {
            b.tvDistance.setText(String.format(Locale.US, "%.2f", distKm));
            b.tvSpeed.setText(String.format(Locale.US, "%.1f", speedKmh));
            b.tvMaxSpeed.setText(String.format(Locale.US, "%.1f", maxSpeedKmh));
            b.tvCalories.setText(String.valueOf(calories));
            b.tvDuration.setText(formatDuration(elapsedMs));
            if (speedKmh > 1f) flashCard(b.cardSpeed);
        });
    }

    @Override
    public void onGpsStatusChanged(boolean hasGps) {
        runOnUiThread(() -> {
            if (hasGps) {
                b.tvGpsStatus.setText(R.string.gps_ready);
                b.tvGpsStatus.setTextColor(getColor(R.color.wave_teal));
                b.gpsIndicator.getBackground().setTint(getColor(R.color.wave_teal));
            } else {
                b.tvGpsStatus.setText(R.string.waiting_gps);
                b.tvGpsStatus.setTextColor(getColor(R.color.text_secondary));
                b.gpsIndicator.getBackground().setTint(getColor(R.color.sun_coral));
            }
        });
    }

    // ── Micro-animations ──────────────────────────────────────────────────────

    private void squishButton() {
        b.btnStartStop.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70)
            .withEndAction(() -> b.btnStartStop.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
            .start();
    }

    private void animateCardsIn() {
        android.view.View[] cards = {b.cardDistance, b.cardSpeed, b.cardCalories, b.cardMaxSpeed};
        long[] delays = {0, 80, 160, 240};
        for (int i = 0; i < cards.length; i++) {
            android.view.View c = cards[i];
            c.setAlpha(0f); c.setTranslationY(40f);
            c.animate().alpha(1f).translationY(0f).setDuration(400)
                .setStartDelay(delays[i]).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void flashCard(android.view.View card) {
        card.animate().alpha(0.4f).setDuration(120)
            .withEndAction(() -> card.animate().alpha(1f).setDuration(180).start()).start();
    }

    private String formatDuration(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacksAndMessages(null);
        stopBreath();
        if (serviceBound) {
            trackingService.setUpdateListener(null);
            unbindService(conn);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
