package com.paddleboard.tracker;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {

    private SessionAdapter adapter;
    private List<SessionData> sessions;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sessions   = SessionStorage.loadAll(this);
        emptyState = findViewById(R.id.emptyState);

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(this, sessions);
        rv.setAdapter(adapter);

        // ── Delete via ✕ button on each card ─────────────────────────────────
        adapter.setOnDeleteListener((pos, session) ->
            new AlertDialog.Builder(this)
                .setTitle("Delete session?")
                .setMessage("Remove this session from history?")
                .setPositiveButton("Delete", (d, w) -> {
                    SessionStorage.delete(this, session.id);
                    adapter.removeAt(pos);
                    updateEmptyState();
                    updateStatsHeader();
                })
                .setNegativeButton("Keep", null)
                .show());

        updateEmptyState();
        updateStatsHeader();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void updateEmptyState() {
        emptyState.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Lifetime totals + badge shelf ─────────────────────────────────────────

    private void updateStatsHeader() {
        View header = findViewById(R.id.statsHeader);
        if (sessions.isEmpty()) { header.setVisibility(View.GONE); return; }
        header.setVisibility(View.VISIBLE);

        Achievements.Totals t = Achievements.totals(sessions);
        ((TextView) findViewById(R.id.tvTotSessions)).setText(String.valueOf(t.sessions));
        ((TextView) findViewById(R.id.tvTotDistance))
            .setText(String.format(Locale.US, "%.1f", t.distanceKm));
        ((TextView) findViewById(R.id.tvTotTime))
            .setText(String.format(Locale.US, "%.1f", t.durationMs / 3_600_000f));
        ((TextView) findViewById(R.id.tvTotCalories)).setText(String.valueOf(t.calories));

        LinearLayout shelf = findViewById(R.id.llBadges);
        shelf.removeAllViews();
        Set<String> owned = Achievements.loadUnlocked(this);
        float dp = getResources().getDisplayMetrics().density;

        for (Achievements.Badge badge : Achievements.ALL) {
            boolean unlocked = owned.contains(badge.id);
            TextView chip = new TextView(this);
            chip.setText(badge.emoji);
            chip.setTextSize(22);
            chip.setGravity(Gravity.CENTER);
            chip.setAlpha(unlocked ? 1f : 0.22f);
            chip.setBackgroundResource(R.drawable.card_bg);
            int size = (int) (46 * dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd((int) (7 * dp));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> Toast.makeText(this,
                badge.emoji + " " + badge.title + (unlocked ? "" : " (locked)")
                    + "\n" + badge.desc,
                Toast.LENGTH_SHORT).show());
            shelf.addView(chip);
        }
    }
}
