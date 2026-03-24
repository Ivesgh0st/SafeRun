package com.saferun.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.saferun.app.R;

public class EmergencyAlert {

    private static final String TAG = "EmergencyAlert";
    private static final String CHANNEL_ID = "saferun_alert_channel";

    // Método principal: envia SMS + notificação
    public static void send(Context context, String phone,
                            String reason, double lat, double lng) {

        // Monta o link do Google Maps com a localização atual
        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lng;

        // Monta a mensagem completa do SMS
        String message = "SAFERUN ALERTA: " + reason
                + "\nLocalizacao: " + mapsLink
                + "\nResponda ESTOU BEM se estiver seguro.";

        // Envia o SMS
        try {
            SmsManager sms = SmsManager.getDefault();
            // divideMessage divide automaticamente se for muito longa
            sms.sendMultipartTextMessage(
                    phone,
                    null,
                    sms.divideMessage(message),
                    null,
                    null);
            Log.d(TAG, "SMS enviado para: " + phone);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao enviar SMS: " + e.getMessage());
        }

        // Exibe notificação local no celular do corredor
        showNotification(context, reason);
    }

    // Cria e exibe a notificação de alerta
    private static void showNotification(Context context, String reason) {

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm == null) return;

        // Android 8+ exige um canal de notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SafeRun Alertas",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        // Constrói e exibe a notificação
        nm.notify(2, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle("Alerta enviado!")
                .setContentText(reason)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build());
    }
}