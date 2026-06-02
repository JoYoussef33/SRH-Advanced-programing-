package com.paddleboard.tracker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
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

        updateEmptyState();

        // ── Swipe to delete ───────────────────────────────────────────────────
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable bg = new ColorDrawable(Color.parseColor("#CC331100"));
            private final Paint paint = buildPaint();

            @Override public boolean onMove(@androidx.annotation.NonNull RecyclerView r,
                    @androidx.annotation.NonNull RecyclerView.ViewHolder v,
                    @androidx.annotation.NonNull RecyclerView.ViewHolder t) { return false; }

            @Override public void onSwiped(@androidx.annotation.NonNull RecyclerView.ViewHolder vh, int d) {
                int pos = vh.getAdapterPosition();
                SessionData s = adapter.getItem(pos);
                SessionStorage.delete(HistoryActivity.this, s.id);
                adapter.removeAt(pos);
                updateEmptyState();
            }

            @Override public void onChildDraw(@androidx.annotation.NonNull Canvas c,
                    @androidx.annotation.NonNull RecyclerView rv,
                    @androidx.annotation.NonNull RecyclerView.ViewHolder vh,
                    float dX, float dY, int state, boolean active) {
                View item = vh.itemView;
                bg.setBounds((int)(item.getRight() + dX), item.getTop(), item.getRight(), item.getBottom());
                bg.draw(c);
                // Draw delete label
                if (dX < -60) {
                    float x = item.getRight() - 80f;
                    float y = item.getTop() + (item.getBottom() - item.getTop()) / 2f + 14f;
                    c.drawText("DELETE", x, y, paint);
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }

            private Paint buildPaint() {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.WHITE);
                p.setTextSize(32f);
                p.setTextAlign(Paint.Align.RIGHT);
                return p;
            }
        }).attachToRecyclerView(rv);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void updateEmptyState() {
        emptyState.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
