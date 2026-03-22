package com.saferun.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.saferun.app.R;
import com.saferun.app.database.RunSession;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RunHistoryAdapter extends RecyclerView.Adapter<RunHistoryAdapter.ViewHolder> {

    private final List<RunSession> sessions;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public RunHistoryAdapter(List<RunSession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_run_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunSession session = sessions.get(position);

        holder.tvDate.setText(dateFormat.format(session.date));

        // Converte metros para quilômetros
        holder.tvDistance.setText(String.format(Locale.getDefault(),
                "%.2f km", session.distanceMeters / 1000));

        // Converte segundos para mm:ss
        int minutes = session.durationSeconds / 60;
        int seconds = session.durationSeconds % 60;
        holder.tvDuration.setText(String.format(Locale.getDefault(),
                "%02d:%02d min", minutes, seconds));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDistance, tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate     = itemView.findViewById(R.id.tv_run_date);
            tvDistance = itemView.findViewById(R.id.tv_run_distance);
            tvDuration = itemView.findViewById(R.id.tv_run_duration);
        }
    }
}