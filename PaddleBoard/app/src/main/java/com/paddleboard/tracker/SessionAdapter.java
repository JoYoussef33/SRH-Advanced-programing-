package com.paddleboard.tracker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {

    private final List<SessionData> items;
    private final Context ctx;

    public SessionAdapter(Context ctx, List<SessionData> items) {
        this.ctx = ctx;
        this.items = items;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_session, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SessionData s = items.get(pos);
        int tier = s.tier();

        h.tvDate.setText(s.formatDate());
        h.tvTier.setText(MessageGenerator.getTierLabel(tier));
        h.tvTier.setTextColor(MessageGenerator.getTierColor(tier));
        h.tvDistance.setText(String.format(Locale.US, "%.2f km", s.distanceKm));
        h.tvDuration.setText(s.formatDuration());
        h.tvCalories.setText(s.calories + " kcal");
        h.tvMaxSpeed.setText(String.format(Locale.US, "⚡ %.1f km/h peak", s.maxSpeedKmh));

        h.itemView.setOnClickListener(v -> {
            try {
                Intent i = new Intent(ctx, SessionEndActivity.class);
                i.putExtra(SessionEndActivity.EXTRA_SESSION, s.toJson().toString());
                i.putExtra(SessionEndActivity.EXTRA_READONLY, true);
                ctx.startActivity(i);
            } catch (JSONException ignored) {}
        });
    }

    @Override public int getItemCount() { return items.size(); }

    public SessionData getItem(int pos) { return items.get(pos); }
    public void removeAt(int pos) { items.remove(pos); notifyItemRemoved(pos); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTier, tvDistance, tvDuration, tvCalories, tvMaxSpeed;
        VH(View v) {
            super(v);
            tvDate     = v.findViewById(R.id.tvHistDate);
            tvTier     = v.findViewById(R.id.tvHistTier);
            tvDistance = v.findViewById(R.id.tvHistDistance);
            tvDuration = v.findViewById(R.id.tvHistDuration);
            tvCalories = v.findViewById(R.id.tvHistCalories);
            tvMaxSpeed = v.findViewById(R.id.tvHistMaxSpeed);
        }
    }
}
