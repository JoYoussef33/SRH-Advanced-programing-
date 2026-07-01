package com.paddleboard.tracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class SessionEndActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION = "session_json";
    public static final String EXTRA_READONLY = "read_only";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_end);

        String json = getIntent().getStringExtra(EXTRA_SESSION);
        boolean readOnly = getIntent().getBooleanExtra(EXTRA_READONLY, false);

        SessionData session;
        try {
            session = SessionData.fromJson(new JSONObject(json));
        } catch (JSONException e) {
            finish();
            return;
        }

        int tier = session.tier();
        String message = MessageGenerator.getMessage(session);

        // ── Photo background ─────────────────────────────────────────────────
        ImageView photo = findViewById(R.id.imgResultPhoto);
        photo.setImageResource(MessageGenerator.getPhotoRes(tier));

        // ── Tier label ───────────────────────────────────────────────────────
        TextView tvTier = findViewById(R.id.tvTierLabel);
        tvTier.setText(MessageGenerator.getTierLabel(tier));
        tvTier.setTextColor(MessageGenerator.getTierColor(tier));

        // ── Message ──────────────────────────────────────────────────────────
        TextView tvMsg = findViewById(R.id.tvMessage);
        tvMsg.setText(message);

        // ── Personal records (only for a fresh session) ──────────────────────
        if (!readOnly) {
            java.util.List<String> records =
                Achievements.newRecords(session, SessionStorage.loadAll(this));
            if (!records.isEmpty()) {
                TextView tvRecords = findViewById(R.id.tvRecords);
                StringBuilder sb = new StringBuilder();
                for (String r : records) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(r);
                }
                tvRecords.setText(sb);
                tvRecords.setVisibility(View.VISIBLE);
            }
        }

        // ── Stats ────────────────────────────────────────────────────────────
        ((TextView) findViewById(R.id.tvResDate)).setText(session.formatDate());
        ((TextView) findViewById(R.id.tvResDistance))
                .setText(String.format(Locale.US, "%.2f km", session.distanceKm));
        ((TextView) findViewById(R.id.tvResDuration)).setText(session.formatDuration());
        ((TextView) findViewById(R.id.tvResCalories))
                .setText(session.calories + " kcal");
        ((TextView) findViewById(R.id.tvResMaxSpeed))
                .setText(String.format(Locale.US, "%.1f km/h", session.maxSpeedKmh));
        ((TextView) findViewById(R.id.tvResStart))
                .setText("Started " + new java.text.SimpleDateFormat("HH:mm",
                        java.util.Locale.getDefault()).format(new java.util.Date(session.startTimeMs)));
        ((TextView) findViewById(R.id.tvResEnd))
                .setText("Ended " + session.formatEndTime());

        // ── Buttons ──────────────────────────────────────────────────────────
        AppCompatButton btnSave    = findViewById(R.id.btnSave);
        AppCompatButton btnDiscard = findViewById(R.id.btnDiscard);

        if (readOnly) {
            btnSave.setVisibility(View.GONE);
            btnDiscard.setText("CLOSE");
            btnDiscard.setOnClickListener(v -> finish());
        } else {
            btnSave.setOnClickListener(v -> {
                SessionStorage.save(this, session);
                java.util.List<Achievements.Badge> fresh =
                    Achievements.checkAndUnlock(this, session, SessionStorage.loadAll(this));
                if (fresh.isEmpty()) {
                    Toast.makeText(this, "Session saved! 🏄", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Achievements.Badge bd : fresh) {
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(bd.emoji).append("  ").append(bd.title)
                          .append("\n").append(bd.desc);
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("🏆 Achievement unlocked!")
                        .setMessage(sb)
                        .setPositiveButton("NICE!", (d, w) -> finish())
                        .setOnDismissListener(d -> finish())
                        .show();
                }
            });
            btnDiscard.setOnClickListener(v -> finish());
        }

        // ── Entrance animation ───────────────────────────────────────────────
        View card = findViewById(R.id.resultCard);
        card.setTranslationY(300f);
        card.setAlpha(0f);
        card.animate().translationY(0f).alpha(1f).setDuration(500)
                .setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
    }
}
