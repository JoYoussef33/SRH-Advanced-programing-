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
import android.view.animation.LinearInterpolator;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.paddleboard.tracker.databinding.ActivityMainBinding;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements TrackingService.OnTrackingUpdateListener {

    private ActivityMainBinding b;
    private TrackingService trackingService;
    private boolean serviceBound = false;
    private boolean sessionActive = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    // Glow animators kept so we can cancel/restart them
    private AnimatorSet coronaAnim;
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

        startIdleGlow();

        b.btnStartStop.setOnClickListener(v -> {
            squishButton();
            if (sessionActive) stopSession();
            else checkPermission();
        });

        Intent svc = new Intent(this, TrackingService.class);
        bindService(svc, conn, Context.BIND_AUTO_CREATE);
    }

    // ── GPS permissions ──────────────────────────────────────────────────────

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startSession();
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // ── Session control ──────────────────────────────────────────────────────

    private void startSession() {
        Intent svc = new Intent(this, TrackingService.class);
        ContextCompat.startForegroundService(this, svc);
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
        if (serviceBound) trackingService.stopTracking();
        sessionActive = false;
        timerHandler.removeCallbacksAndMessages(null);
        applyIdleState();
        b.tvStatus.setText("Great session! 🏄 Rest up.");
        flashCard(b.cardDistance);
        flashCard(b.cardSpeed);
        flashCard(b.cardCalories);
        flashCard(b.cardMaxSpeed);
    }

    // ── Button visual states ─────────────────────────────────────────────────

    private void applyIdleState() {
        b.btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_circle_cyan));
        b.btnStartStop.setText("START\nSESSION");
        b.btnStartStop.setTextColor(getColor(R.color.ocean_deep));
        b.btnGlowRing.setBackground(ContextCompat.getDrawable(this, R.drawable.glow_cyan));
        stopAllGlow();
        startIdleGlow();
    }

    private void applyActiveState() {
        b.btnStartStop.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_circle_coral));
        b.btnStartStop.setText("STOP\nSESSION");
        b.btnStartStop.setTextColor(Color.WHITE);
        b.btnGlowRing.setBackground(ContextCompat.getDrawable(this, R.drawable.glow_coral));
        stopAllGlow();
        startActiveGlow();
    }

    // ── Glow animations ──────────────────────────────────────────────────────

    private void startIdleGlow() {
        // Corona: expands out and fades (pulse every 2.5 s)
        ObjectAnimator cScaleX = pulse(b.btnGlowRing, "scaleX", 1f, 1.8f, 2500);
        ObjectAnimator cScaleY = pulse(b.btnGlowRing, "scaleY", 1f, 1.8f, 2500);
        ObjectAnimator cAlpha  = pulse(b.btnGlowRing, "alpha",  0.8f, 0f,  2500);

        coronaAnim = new AnimatorSet();
        coronaAnim.playTogether(cScaleX, cScaleY, cAlpha);
        coronaAnim.start();

        // Button: slow breathe
        ObjectAnimator bX = breathe(b.btnStartStop, "scaleX", 0.94f, 1.06f, 1600);
        ObjectAnimator bY = breathe(b.btnStartStop, "scaleY", 0.94f, 1.06f, 1600);
        breatheAnim = new AnimatorSet();
        breatheAnim.playTogether(bX, bY);
        breatheAnim.start();
    }

    private void startActiveGlow() {
        // Faster, more urgent corona when recording
        ObjectAnimator cScaleX = pulse(b.btnGlowRing, "scaleX", 1f, 2.0f, 1400);
        ObjectAnimator cScaleY = pulse(b.btnGlowRing, "scaleY", 1f, 2.0f, 1400);
        ObjectAnimator cAlpha  = pulse(b.btnGlowRing, "alpha",  0.9f, 0f,  1400);

        coronaAnim = new AnimatorSet();
        coronaAnim.playTogether(cScaleX, cScaleY, cAlpha);
        coronaAnim.start();

        // Button: quicker breathe
        ObjectAnimator bX = breathe(b.btnStartStop, "scaleX", 0.96f, 1.04f, 900);
        ObjectAnimator bY = breathe(b.btnStartStop, "scaleY", 0.96f, 1.04f, 900);
        breatheAnim = new AnimatorSet();
        breatheAnim.playTogether(bX, bY);
        breatheAnim.start();
    }

    private void stopAllGlow() {
        if (coronaAnim != null) { coronaAnim.cancel(); coronaAnim = null; }
        if (breatheAnim != null) { breatheAnim.cancel(); breatheAnim = null; }
        b.btnGlowRing.setAlpha(1f);
        b.btnGlowRing.setScaleX(1f);
        b.btnGlowRing.setScaleY(1f);
        b.btnStartStop.setScaleX(1f);
        b.btnStartStop.setScaleY(1f);
    }

    private ObjectAnimator pulse(android.view.View v, String prop, float from, float to, long dur) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new DecelerateInterpolator());
        return a;
    }

    private ObjectAnimator breathe(android.view.View v, String prop, float from, float to, long dur) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        return a;
    }

    // ── Timer tick ───────────────────────────────────────────────────────────

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

    // ── TrackingService callbacks ─────────────────────────────────────────────

    @Override
    public void onUpdate(float distKm, float speedKmh, float maxSpeedKmh,
                         int calories, long elapsedMs) {
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

    // ── Micro-animations ─────────────────────────────────────────────────────

    private void squishButton() {
        b.btnStartStop.animate()
            .scaleX(0.88f).scaleY(0.88f).setDuration(70)
            .withEndAction(() ->
                b.btnStartStop.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
            .start();
    }

    private void animateCardsIn() {
        long[] delays = {0, 80, 160, 240};
        android.view.View[] cards = {b.cardDistance, b.cardSpeed, b.cardCalories, b.cardMaxSpeed};
        for (int i = 0; i < cards.length; i++) {
            android.view.View c = cards[i];
            c.setAlpha(0f);
            c.setTranslationY(40f);
            c.animate().alpha(1f).translationY(0f).setDuration(400)
                .setStartDelay(delays[i])
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
    }

    private void flashCard(android.view.View card) {
        card.animate().alpha(0.4f).setDuration(120)
            .withEndAction(() -> card.animate().alpha(1f).setDuration(180).start())
            .start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatDuration(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacksAndMessages(null);
        stopAllGlow();
        if (serviceBound) {
            trackingService.setUpdateListener(null);
            unbindService(conn);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
