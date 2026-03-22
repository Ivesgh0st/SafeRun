package com.saferun.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.saferun.app.R;
import com.saferun.app.RunActivity;
import com.saferun.app.database.AppDatabase;
import com.saferun.app.database.RunSession;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView tvTotalRuns;
    private TextView tvTotalDistance;
    private TextView tvEmergencyContact;
    private MaterialButton btnStartRun;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalRuns       = view.findViewById(R.id.tv_total_runs);
        tvTotalDistance   = view.findViewById(R.id.tv_total_distance);
        tvEmergencyContact= view.findViewById(R.id.tv_emergency_contact);
        btnStartRun       = view.findViewById(R.id.btn_start_run);

        // Ao clicar no botão, abre a tela de corrida
        btnStartRun.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), RunActivity.class)));

        loadStats();
    }

    // Carrega estatísticas do banco em background
    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<RunSession> sessions = db.runSessionDao().getAllSessions();

            int totalRuns = sessions.size();
            double totalDistance = 0;
            for (RunSession s : sessions) {
                totalDistance += s.distanceMeters;
            }

            String contact = requireContext()
                    .getSharedPreferences("saferun_prefs", 0)
                    .getString("emergency_name", "Não configurado");

            final double finalDist = totalDistance;
            final String finalContact = contact;

            // Atualiza a tela na thread principal
            requireActivity().runOnUiThread(() -> {
                tvTotalRuns.setText(String.valueOf(totalRuns));
                tvTotalDistance.setText(String.format("%.1f km", finalDist / 1000));
                tvEmergencyContact.setText(finalContact);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats(); // Atualiza ao voltar para esta tela
    }
}