package com.das.controlgastos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

public class LocationTrackingService extends Service {

    public static final String ACTION_DISTANCE_UPDATE = "com.das.controlgastos.DISTANCE_UPDATE";
    public static final String EXTRA_DISTANCE = "distance";

    private static final String CHANNEL_ID = "tracking_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private double gastoLatitud = 0.0;
    private double gastoLongitud = 0.0;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        gastoLatitud = intent.getDoubleExtra("latitud", 0.0);
        gastoLongitud = intent.getDoubleExtra("longitud", 0.0);

        if (gastoLatitud == 0.0 && gastoLongitud == 0.0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = crearNotificacion("Calculando distancia al gasto...");
        startForeground(NOTIFICATION_ID, notification);

        iniciarActualizacionesUbicacion();

        return START_STICKY;
    }

    private void iniciarActualizacionesUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000
        )
                .setMinUpdateIntervalMillis(2000)
                .setWaitForAccurateLocation(false)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location ubicacionActual = locationResult.getLastLocation();

                if (ubicacionActual == null) {
                    return;
                }

                float[] results = new float[1];

                Location.distanceBetween(
                        ubicacionActual.getLatitude(),
                        ubicacionActual.getLongitude(),
                        gastoLatitud,
                        gastoLongitud,
                        results
                );

                float distancia = results[0];

                enviarDistancia(distancia);
                actualizarNotificacion(distancia);
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
    }

    private void enviarDistancia(float distancia) {
        Intent broadcastIntent = new Intent(ACTION_DISTANCE_UPDATE);
        broadcastIntent.setPackage(getPackageName());
        broadcastIntent.putExtra(EXTRA_DISTANCE, distancia);
        sendBroadcast(broadcastIntent);
    }

    private void actualizarNotificacion(float distancia) {
        Notification notification = crearNotificacion(formatearDistancia(distancia));

        NotificationManager manager = getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification crearNotificacion(String texto) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Seguimiento activo")
                .setContentText(texto)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private String formatearDistancia(float distancia) {
        if (distancia < 1000) {
            return "Distancia al gasto: " + (int) distancia + " m";
        } else {
            return "Distancia al gasto: " +
                    String.format(Locale.getDefault(), "%.2f", distancia / 1000f) +
                    " km";
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Seguimiento de distancia",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Calcula la distancia entre tu ubicación actual y el gasto");

            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}