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
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
    private final Handler tipHandler   = new Handler(Looper.getMainLooper());
    private AnimatorSet breatheAnim;
    private int tipIndex;

    private final ActivityResultLauncher<String> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startSession();
            else Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_LONG).show();
        });

    private final ActivityResultLauncher<String> condPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) refreshConditions();
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

        b.btnProfile.setOnClickListener(v -> showWeightDialog());
        b.cardConditions.setOnClickListener(v -> refreshConditions());

        startTipRotation();
        refreshConditions();

        bindService(new Intent(this, TrackingService.class), conn, Context.BIND_AUTO_CREATE);
    }

    // ── Live conditions (Open-Meteo, free & keyless) ──────────────────────────

    private void refreshConditions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // show any cached data; fetch permission on tap
            WeatherData cached = WeatherRepo.loadCached(this);
            if (cached != null) showConditions(cached, true);
            else b.tvVerdict.setText("Tap to check the water 🌊");
            b.cardConditions.setOnClickListener(v ->
                condPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));
            return;
        }
        b.cardConditions.setOnClickListener(v -> refreshConditions());

        Location loc = lastKnownLocation();
        if (loc == null) {
            WeatherData cached = WeatherRepo.loadCached(this);
            if (cached != null) showConditions(cached, true);
            else b.tvVerdict.setText("Waiting for a GPS fix… 📡");
            return;
        }

        b.tvVerdict.setText("Reading the ocean… 🌊");
        WeatherRepo.fetch(this, loc.getLatitude(), loc.getLongitude(), (data, fromCache) -> {
            if (data != null) showConditions(data, fromCache);
            else b.tvVerdict.setText("No connection — try again later 📶");
        });
    }

    private Location lastKnownLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            for (String p : new String[]{LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null) return l;
            }
        } catch (SecurityException ignored) {}
        return null;
    }

    private void showConditions(WeatherData w, boolean fromCache) {
        b.tvVerdict.setText(w.verdict());
        b.tvPaddleScore.setText(String.valueOf(w.score()));
        b.tvPaddleScore.setTextColor(w.scoreColor());
        b.scoreBadge.setVisibility(android.view.View.VISIBLE);

        StringBuilder line1 = new StringBuilder();
        if (!Float.isNaN(w.windKmh)) {
            line1.append(String.format(Locale.US, "💨 %.0f km/h %s", w.windKmh, w.windCompass()));
            if (!Float.isNaN(w.gustKmh))
                line1.append(String.format(Locale.US, " (gusts %.0f)", w.gustKmh));
        }
        if (!Float.isNaN(w.airTempC))
            line1.append(String.format(Locale.US, "  ·  🌡 %.0f°C", w.airTempC));
        if (!Float.isNaN(w.uvIndex))
            line1.append(String.format(Locale.US, "  ·  ☀️ UV %.0f", w.uvIndex));

        StringBuilder line2 = new StringBuilder();
        if (!Float.isNaN(w.waveHeightM))
            line2.append(String.format(Locale.US, "🌊 %.1f m waves", w.waveHeightM));
        if (!Float.isNaN(w.waterTempC)) {
            if (line2.length() > 0) line2.append("  ·  ");
            line2.append(String.format(Locale.US, "💧 %.0f°C water", w.waterTempC));
        }
        if (!w.sunrise.isEmpty()) {
            if (line2.length() > 0) line2.append("  ·  ");
            line2.append("🌅 ").append(w.sunrise).append("  🌇 ").append(w.sunset);
        }

        String detail = line1.toString();
        if (line2.length() > 0) detail += "\n" + line2;
        b.tvConditionsDetail.setText(detail);
        b.tvConditionsDetail.setVisibility(android.view.View.VISIBLE);

        long ageMin = (System.currentTimeMillis() - w.fetchedAtMs) / 60_000L;
        b.tvConditionsAge.setText(ageMin < 1 ? "updated just now"
                : "updated " + ageMin + " min ago · tap to refresh");
        b.tvConditionsAge.setVisibility(android.view.View.VISIBLE);
    }

    // ── Rotating SUP tips ─────────────────────────────────────────────────────

    private void startTipRotation() {
        tipIndex = new java.util.Random().nextInt(PaddleFacts.count());
        b.tvTip.setText(PaddleFacts.next(tipIndex));
        tipHandler.postDelayed(new Runnable() {
            @Override public void run() {
                b.tvTip.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                    b.tvTip.setText(PaddleFacts.next(++tipIndex));
                    b.tvTip.animate().alpha(1f).setDuration(400).start();
                }).start();
                tipHandler.postDelayed(this, 10_000);
            }
        }, 10_000);
    }

    // ── Weight profile ────────────────────────────────────────────────────────

    private void showWeightDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.0f", UserProfile.getWeightKg(this)));
        input.setSelection(input.getText().length());

        FrameLayout wrap = new FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        wrap.setPadding(pad, 0, pad, 0);
        wrap.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("Your weight (kg)")
            .setMessage("Used for accurate calorie tracking (MET formula).")
            .setView(wrap)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    float kg = Float.parseFloat(input.getText().toString().trim());
                    if (kg >= 30f && kg <= 250f) {
                        UserProfile.setWeightKg(this, kg);
                        Toast.makeText(this, "Saved — calories now tuned to you 💪",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Enter a weight between 30 and 250 kg",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        tipHandler.removeCallbacksAndMessages(null);
        stopBreath();
        if (serviceBound) {
            trackingService.setUpdateListener(null);
            unbindService(conn);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
