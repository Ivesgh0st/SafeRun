package com.saferun.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.saferun.app.MainActivity;
import com.saferun.app.R;
import com.saferun.app.database.AppDatabase;
import com.saferun.app.database.RunSession;
import com.saferun.app.utils.EmergencyAlert;
import com.saferun.app.utils.FallDetector;
import java.util.Date;
import java.util.concurrent.Executors;

public class RunningService extends Service implements SensorEventListener {

    // ── Constantes ───────────────────────────────────────────────────────
    private static final String CHANNEL_ID = "saferun_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long LOCATION_INTERVAL_MS = 3000;

    // ── Binder ───────────────────────────────────────────────────────────
    private final IBinder binder = new RunBinder();

    public class RunBinder extends Binder {
        public RunningService getService() {
            return RunningService.this;
        }
    }

    // ── Interface de callback ────────────────────────────────────────────
    public interface RunningCallback {
        void onDistanceUpdated(double distanceMeters);
        void onSpeedUpdated(float speedKmh);
        void onFallDetected();
        void onSuspiciousStop();
        void onLocationChanged(double lat, double lng);
    }

    private RunningCallback callback;

    // ── GPS ──────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private double totalDistance = 0;
    private long lastMovementTime;
    private double startLat, startLng;

    // ── Acelerômetro ─────────────────────────────────────────────────────
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FallDetector fallDetector;

    // ── Controle de estado ───────────────────────────────────────────────
    private boolean isTracking = false;
    private boolean alertCancelled = false;
    private boolean sessionSaved = false;
    private long startTimeMillis;
    private long pausedElapsed = 0;
    private long pauseStart = 0;

    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private Runnable stopCheckRunnable;

    // ────────────────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        fallDetector = new FallDetector();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startTracking();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // ── Inicia o rastreamento ────────────────────────────────────────────
    public void startTracking() {
        if (isTracking) return;
        isTracking = true;
        alertCancelled = false;
        startTimeMillis = SystemClock.elapsedRealtime();
        lastMovementTime = System.currentTimeMillis();

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateDistanceMeters(0.5f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) {
                    processLocation(loc);
                }
            }
        };

        try {
            fusedClient.requestLocationUpdates(request,
                    locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        scheduleStopCheck();
    }

    // ── Pausa ────────────────────────────────────────────────────────────
    public void pauseTracking() {
        isTracking = false;
        pauseStart = SystemClock.elapsedRealtime();
        fusedClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);
        stopHandler.removeCallbacks(stopCheckRunnable);
    }

    // ── Retoma ───────────────────────────────────────────────────────────
    public void resumeTracking() {
        pausedElapsed += SystemClock.elapsedRealtime() - pauseStart;
        startTracking();
    }

    // ── Para e salva ─────────────────────────────────────────────────────
    public void stopTracking() {
        isTracking = false;
        fusedClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);
        stopHandler.removeCallbacks(stopCheckRunnable);
        saveSession();
    }

    public void cancelAlert() { alertCancelled = true; }
    public void setCallback(RunningCallback cb) { this.callback = cb; }

    public long getElapsedMillis() {
        return SystemClock.elapsedRealtime() - startTimeMillis - pausedElapsed;
    }

    // ── Processa nova posição GPS ────────────────────────────────────────
    private void processLocation(Location loc) {
        // Ignora leituras com precisão ruim
        if (loc.getAccuracy() > 20f) return;

        if (lastLocation == null) {
            startLat = loc.getLatitude();
            startLng = loc.getLongitude();
            lastLocation = loc;
            // Atualiza o mapa mesmo na primeira leitura
            if (callback != null) {
                callback.onLocationChanged(
                        loc.getLatitude(), loc.getLongitude());
            }
            return;
        }

        float distance = lastLocation.distanceTo(loc);

        if (distance > 0.5f) {
            totalDistance += distance;
            lastMovementTime = System.currentTimeMillis();
            lastLocation = loc;

            if (callback != null) {
                callback.onDistanceUpdated(totalDistance);
                callback.onSpeedUpdated(loc.getSpeed() * 3.6f);
                callback.onLocationChanged(
                        loc.getLatitude(), loc.getLongitude());
            }
        }
    }

    // ── Acelerômetro ─────────────────────────────────────────────────────
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        if (!isTracking || alertCancelled) return;

        if (fallDetector.detect(
                event.values[0], event.values[1], event.values[2])) {
            isTracking = false;
            if (callback != null) callback.onFallDetected();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!alertCancelled) {
                    sendAlert("Possivel queda detectada durante corrida.");
                }
                alertCancelled = false;
                isTracking = true;
            }, 30000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ── Detecção de parada ───────────────────────────────────────────────
    private void scheduleStopCheck() {
        stopCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTracking) return;

                long timeout = getSharedPreferences("saferun_prefs", 0)
                        .getInt("stop_timeout_seconds", 30) * 1000L;

                long parado = System.currentTimeMillis() - lastMovementTime;

                if (parado >= timeout && !alertCancelled) {
                    isTracking = false;
                    if (callback != null) callback.onSuspiciousStop();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!alertCancelled) {
                            sendAlert("Parada prolongada detectada.");
                        }
                        alertCancelled = false;
                        isTracking = true;
                        lastMovementTime = System.currentTimeMillis();
                    }, 30000);
                }

                stopHandler.postDelayed(this, 10000);
            }
        };
        stopHandler.postDelayed(stopCheckRunnable, 10000);
    }

    // ── Alerta de emergência ─────────────────────────────────────────────
    private void sendAlert(String reason) {
        String phone = getSharedPreferences("saferun_prefs", 0)
                .getString("emergency_phone", "");

        if (!phone.isEmpty() && lastLocation != null) {
            EmergencyAlert.send(this, phone, reason,
                    lastLocation.getLatitude(),
                    lastLocation.getLongitude());
        }
    }

    // ── Salva sessão no banco ────────────────────────────────────────────
    private void saveSession() {
        if (sessionSaved) return;
        sessionSaved = true;

        RunSession session = new RunSession();
        session.date = new Date();
        session.distanceMeters = totalDistance;
        session.durationSeconds = (int) (getElapsedMillis() / 1000);
        session.startLat = startLat;
        session.startLng = startLng;

        Executors.newSingleThreadExecutor().execute(() ->
                AppDatabase.getInstance(this).runSessionDao().insert(session));
    }

    // ── Notificação persistente ──────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SafeRun Ativo",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeRun em andamento")
                .setContentText("Monitorando sua corrida com seguranca")
                .setSmallIcon(R.drawable.ic_run)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
    }
}