package com.das.controlgastos.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.das.controlgastos.R;

public class NotificationHelper {

    private static final String CHANNEL_ID = "gastos_channel";

    public static void createChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorio de gastos",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription("Notificaciones para registrar gastos");

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNotification(Context context) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Control de gastos")
                        .setContentText("No olvides registrar tus gastos de hoy")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);

        manager.notify(1, builder.build());
    }
}