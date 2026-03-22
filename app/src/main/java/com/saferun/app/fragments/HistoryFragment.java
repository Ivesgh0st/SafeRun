package com.saferun.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.saferun.app.R;
import com.saferun.app.adapters.RunHistoryAdapter;
import com.saferun.app.database.AppDatabase;
import com.saferun.app.database.RunSession;
import java.util.List;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_history);
        tvEmpty      = view.findViewById(R.id.tv_empty_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadHistory();
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<RunSession> sessions = AppDatabase
                    .getInstance(requireContext())
                    .runSessionDao()
                    .getAllSessionsOrderedByDate();

            requireActivity().runOnUiThread(() -> {
                if (sessions.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(new RunHistoryAdapter(sessions));
                }
            });
        });
    }
}