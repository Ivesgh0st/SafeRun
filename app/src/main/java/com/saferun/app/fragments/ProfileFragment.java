package com.saferun.app.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.saferun.app.R;

public class ProfileFragment extends Fragment {

    private TextInputEditText etUserName;
    private TextInputEditText etEmergencyName;
    private TextInputEditText etEmergencyPhone;
    private TextInputEditText etStopTimeout;
    private MaterialButton btnSave;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences("saferun_prefs", 0);

        etUserName      = view.findViewById(R.id.et_user_name);
        etEmergencyName = view.findViewById(R.id.et_emergency_name);
        etEmergencyPhone= view.findViewById(R.id.et_emergency_phone);
        etStopTimeout   = view.findViewById(R.id.et_stop_timeout);
        btnSave         = view.findViewById(R.id.btn_save_profile);

        loadSavedData();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    // Preenche os campos com dados já salvos
    private void loadSavedData() {
        etUserName.setText(prefs.getString("user_name", ""));
        etEmergencyName.setText(prefs.getString("emergency_name", ""));
        etEmergencyPhone.setText(prefs.getString("emergency_phone", ""));
        etStopTimeout.setText(String.valueOf(
                prefs.getInt("stop_timeout_seconds", 30)));
    }

    // Valida e salva os dados
    private void saveProfile() {
        String userName      = etUserName.getText().toString().trim();
        String emergencyName = etEmergencyName.getText().toString().trim();
        String emergencyPhone= etEmergencyPhone.getText().toString().trim();
        String timeoutStr    = etStopTimeout.getText().toString().trim();

        if (TextUtils.isEmpty(emergencyName) || TextUtils.isEmpty(emergencyPhone)) {
            Toast.makeText(getContext(),
                    "Preencha o contato de emergência!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (emergencyPhone.length() < 10) {
            Toast.makeText(getContext(),
                    "Telefone inválido. Use DDD + número.", Toast.LENGTH_SHORT).show();
            return;
        }

        int timeout = 30;
        try {
            timeout = Integer.parseInt(timeoutStr);
            if (timeout < 10) timeout = 10;
        } catch (NumberFormatException ignored) {}

        prefs.edit()
                .putString("user_name", userName)
                .putString("emergency_name", emergencyName)
                .putString("emergency_phone", emergencyPhone)
                .putInt("stop_timeout_seconds", timeout)
                .apply();

        Toast.makeText(getContext(), "Perfil salvo!", Toast.LENGTH_SHORT).show();
    }
}