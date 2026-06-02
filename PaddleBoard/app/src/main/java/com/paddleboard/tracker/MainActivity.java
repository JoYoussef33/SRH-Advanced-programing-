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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.paddleboard.tracker.databinding.ActivityMainBinding;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TrackingService.OnTrackingUpdateListener {

    private ActivityMainBinding binding;
    private TrackingService trackingService;
    private boolean serviceBound = false;
    private boolean sessionActive = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Animation breatheAnim;
    private Animation pulseAnim;
    private AnimatorSet glowAnimSet;

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startSession();
            else Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_LONG).show();
        });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TrackingService.LocalBinder lb = (TrackingService.LocalBinder) binder;
            trackingService = lb.getService();
            trackingService.setUpdateListener(MainActivity.this);
            serviceBound = true;

            if (trackingService.isTracking()) {
                sessionActive = true;
                updateButtonState(true);
                startTimerDisplay();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        breatheAnim = AnimationUtils.loadAnimation(this, R.anim.breathe);
        pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);

        setupGlowAnimation();
        binding.btnStartStop.startAnimation(breatheAnim);
        binding.btnGlowRing.startAnimation(pulseAnim);

        binding.btnStartStop.setOnClickListener(v -> {
            animateButtonPress(v);
            if (sessionActive) stopSession();
            else checkPermissionAndStart();
        });

        // Bind to service
        Intent serviceIntent = new Intent(this, TrackingService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startSession();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startSession() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        if (serviceBound) {
            trackingService.startTracking();
            sessionActive = true;
            updateButtonState(true);
            startTimerDisplay();
            animateCardsIn();
            binding.tvStatus.setText("Session active — paddle hard! 🌊");
        }
    }

    private void stopSession() {
        if (serviceBound) {
            trackingService.stopTracking();
        }
        sessionActive = false;
        timerHandler.removeCallbacksAndMessages(null);
        updateButtonState(false);
        binding.tvStatus.setText("Great session! 🏄 Rest up.");

        // Show final stats flash
        flashCard(binding.cardDistance);
        flashCard(binding.cardSpeed);
        flashCard(binding.cardCalories);
        flashCard(binding.cardMaxSpeed);
    }

    private void updateButtonState(boolean active) {
        if (active) {
            binding.btnStartStop.clearAnimation();
            binding.btnGlowRing.clearAnimation();
            binding.btnStartStop.setBackgroundResource(R.drawable.btn_stop_bg);
            binding.btnGlowRing.setBackgroundResource(R.drawable.btn_stop_bg);
            binding.btnStartStop.setText(R.string.stop_session);
            binding.btnStartStop.setTextColor(Color.WHITE);

            // Faster pulse when active
            Animation activePulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            activePulse.setDuration(800);
            binding.btnStartStop.startAnimation(activePulse);
            binding.btnGlowRing.startAnimation(pulseAnim);
        } else {
            binding.btnStartStop.clearAnimation();
            binding.btnGlowRing.clearAnimation();
            binding.btnStartStop.setBackgroundResource(R.drawable.btn_start_bg);
            binding.btnGlowRing.setBackgroundResource(R.drawable.btn_start_bg);
            binding.btnStartStop.setText(R.string.start_session);
            binding.btnStartStop.setTextColor(getColor(R.color.ocean_deep));
            binding.btnStartStop.startAnimation(breatheAnim);
            binding.btnGlowRing.startAnimation(pulseAnim);
        }
    }

    private void startTimerDisplay() {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!sessionActive || !serviceBound) return;
                long elapsed = System.currentTimeMillis() - trackingService.getSessionStartTime();
                binding.tvDuration.setText(formatDuration(elapsed));
                // Also refresh calories on timer tick (GPS updates may be sparse)
                binding.tvCalories.setText(String.valueOf(trackingService.getCalories()));
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onUpdate(float distanceKm, float speedKmh, float maxSpeedKmh, int calories, long elapsedMs) {
        runOnUiThread(() -> {
            binding.tvDistance.setText(String.format(Locale.US, "%.2f", distanceKm));
            binding.tvSpeed.setText(String.format(Locale.US, "%.1f", speedKmh));
            binding.tvMaxSpeed.setText(String.format(Locale.US, "%.1f", maxSpeedKmh));
            binding.tvCalories.setText(String.valueOf(calories));
            binding.tvDuration.setText(formatDuration(elapsedMs));

            // Pulse speed card when moving
            if (speedKmh > 0.5f) {
                flashCard(binding.cardSpeed);
            }
        });
    }

    @Override
    public void onGpsStatusChanged(boolean hasGps) {
        runOnUiThread(() -> {
            if (hasGps) {
                binding.tvGpsStatus.setText(R.string.gps_ready);
                binding.tvGpsStatus.setTextColor(getColor(R.color.wave_teal));
                binding.gpsIndicator.setBackgroundResource(R.drawable.gps_indicator);
                // Make indicator green
                binding.gpsIndicator.getBackground().setTint(getColor(R.color.wave_teal));
                binding.gpsIndicator.startAnimation(pulseAnim);
            } else {
                binding.tvGpsStatus.setText(R.string.waiting_gps);
                binding.tvGpsStatus.setTextColor(getColor(R.color.text_secondary));
                binding.gpsIndicator.clearAnimation();
                binding.gpsIndicator.getBackground().setTint(getColor(R.color.sun_coral));
            }
        });
    }

    private void setupGlowAnimation() {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(binding.btnGlowRing, "alpha", 0.2f, 0.7f);
        alphaAnim.setDuration(1500);
        alphaAnim.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnim.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.btnGlowRing, "scaleX", 0.95f, 1.15f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.btnGlowRing, "scaleY", 0.95f, 1.15f);
        scaleX.setDuration(1500);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setDuration(1500);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        glowAnimSet = new AnimatorSet();
        glowAnimSet.playTogether(alphaAnim, scaleX, scaleY);
        glowAnimSet.start();
    }

    private void animateButtonPress(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(
            () -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        ).start();
    }

    private void animateCardsIn() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        binding.cardDistance.startAnimation(slideUp);
        Animation slideUp2 = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp2.setStartOffset(80);
        binding.cardSpeed.startAnimation(slideUp2);
        Animation slideUp3 = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp3.setStartOffset(160);
        binding.cardCalories.startAnimation(slideUp3);
        Animation slideUp4 = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp4.setStartOffset(240);
        binding.cardMaxSpeed.startAnimation(slideUp4);
    }

    private void flashCard(View card) {
        card.animate().alpha(0.5f).setDuration(150).withEndAction(
            () -> card.animate().alpha(1f).setDuration(150).start()
        ).start();
    }

    private String formatDuration(long ms) {
        long secs = ms / 1000;
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacksAndMessages(null);
        if (glowAnimSet != null) glowAnimSet.cancel();
        if (serviceBound) {
            trackingService.setUpdateListener(null);
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
