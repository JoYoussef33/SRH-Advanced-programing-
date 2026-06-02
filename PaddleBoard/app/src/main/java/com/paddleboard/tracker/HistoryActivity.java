package com.paddleboard.tracker;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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
                })
                .setNegativeButton("Keep", null)
                .show());

        updateEmptyState();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void updateEmptyState() {
        emptyState.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
