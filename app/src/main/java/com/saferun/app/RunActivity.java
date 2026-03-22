package com.saferun.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.saferun.app.services.RunningService;

public class RunActivity extends AppCompatActivity
        implements RunningService.RunningCallback {

    private TextView tvDistance;
    private TextView tvSpeed;
    private TextView tvStatus;
    private Chronometer chronometer;
    private MaterialButton btnStop;
    private MaterialButton btnPause;

    private RunningService runningService;
    private boolean serviceBound = false;
    private boolean isPaused = false;

    // Conexão com o RunningService
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            RunningService.RunBinder runBinder = (RunningService.RunBinder) binder;
            runningService = runBinder.getService();
            runningService.setCallback(RunActivity.this);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);

        // Liga os elementos visuais às variáveis
        tvDistance  = findViewById(R.id.tv_distance);
        tvSpeed     = findViewById(R.id.tv_speed);
        tvStatus    = findViewById(R.id.tv_status);
        chronometer = findViewById(R.id.chronometer);
        btnStop     = findViewById(R.id.btn_stop_run);
        btnPause    = findViewById(R.id.btn_pause_run);

        // Inicia e conecta o serviço de monitoramento
        Intent serviceIntent = new Intent(this, RunningService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        // Inicia o cronômetro
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        btnPause.setOnClickListener(v -> togglePause());
        btnStop.setOnClickListener(v -> confirmStop());
    }

    // Alterna entre pausar e retomar a corrida
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            chronometer.stop();
            btnPause.setText("Retomar");
            tvStatus.setText("Pausado");
            if (serviceBound) runningService.pauseTracking();
        } else {
            chronometer.setBase(SystemClock.elapsedRealtime()
                    - (serviceBound ? runningService.getElapsedMillis() : 0));
            chronometer.start();
            btnPause.setText("Pausar");
            tvStatus.setText("Correndo");
            if (serviceBound) runningService.resumeTracking();
        }
    }

    // Pede confirmação antes de encerrar
    private void confirmStop() {
        new AlertDialog.Builder(this)
                .setTitle("Encerrar corrida?")
                .setMessage("Sua corrida será salva no histórico.")
                .setPositiveButton("Encerrar", (d, w) -> stopRun())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Encerra a corrida e volta para a tela principal
    private void stopRun() {
        chronometer.stop();
        if (serviceBound) {
            runningService.stopTracking();
            unbindService(serviceConnection);
            serviceBound = false;
        }
        stopService(new Intent(this, RunningService.class));
        finish();
    }

    // ── Callbacks do RunningService ──────────────────────────────────────

    @Override
    public void onDistanceUpdated(double distanceMeters) {
        runOnUiThread(() ->
                tvDistance.setText(String.format("%.0f m", distanceMeters)));
    }

    @Override
    public void onSpeedUpdated(float speedKmh) {
        runOnUiThread(() ->
                tvSpeed.setText(String.format("%.1f km/h", speedKmh)));
    }

    @Override
    public void onFallDetected() {
        runOnUiThread(() -> {
            tvStatus.setText("⚠ QUEDA DETECTADA!");
            showEmergencyCountdown("Queda detectada!");
        });
    }

    @Override
    public void onSuspiciousStop() {
        runOnUiThread(() -> {
            tvStatus.setText("⚠ PARADA SUSPEITA!");
            showEmergencyCountdown("Parada prolongada detectada!");
        });
    }

    // Diálogo de contagem regressiva antes do alerta
    private void showEmergencyCountdown(String reason) {
        new AlertDialog.Builder(this)
                .setTitle("🚨 " + reason)
                .setMessage("Alerta será enviado em 30 segundos.\nToque CANCELAR se estiver bem.")
                .setPositiveButton("CANCELAR ALERTA", (d, w) -> {
                    tvStatus.setText("Correndo");
                    if (serviceBound) runningService.cancelAlert();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}