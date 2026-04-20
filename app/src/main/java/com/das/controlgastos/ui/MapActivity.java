package com.das.controlgastos.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.das.controlgastos.LocationTrackingService;
import com.das.controlgastos.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private TextView tvDistancia;
    private Button btnSeguimiento;

    private double latitud;
    private double longitud;
    private String titulo;

    private boolean seguimientoActivo = false;
    private boolean receiverRegistrado = false;

    private final BroadcastReceiver distanceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationTrackingService.ACTION_DISTANCE_UPDATE.equals(intent.getAction())) {
                float distancia = intent.getFloatExtra(LocationTrackingService.EXTRA_DISTANCE, -1);

                if (distancia >= 0) {
                    actualizarTextoDistancia(distancia);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        tvDistancia = findViewById(R.id.tvDistancia);
        btnSeguimiento = findViewById(R.id.btnSeguimiento);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        latitud = getIntent().getDoubleExtra("latitud", 0.0);
        longitud = getIntent().getDoubleExtra("longitud", 0.0);
        titulo = getIntent().getStringExtra("titulo");

        mostrarUbicacionDelGasto();
        configurarEstadoBoton(false);

        btnSeguimiento.setOnClickListener(v -> {
            if (!seguimientoActivo) {
                Intent serviceIntent = new Intent(MapActivity.this, LocationTrackingService.class);
                serviceIntent.putExtra("latitud", latitud);
                serviceIntent.putExtra("longitud", longitud);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                seguimientoActivo = true;
                configurarEstadoBoton(true);
            } else {
                Intent serviceIntent = new Intent(MapActivity.this, LocationTrackingService.class);
                stopService(serviceIntent);

                seguimientoActivo = false;
                configurarEstadoBoton(false);
                tvDistancia.setText("📍 Distancia actual: --");
            }
        });
    }

    private void mostrarUbicacionDelGasto() {
        if (latitud == 0.0 && longitud == 0.0) {
            Toast.makeText(this, "Este gasto no tiene ubicación guardada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        GeoPoint puntoGasto = new GeoPoint(latitud, longitud);

        map.getController().setZoom(18.0);
        map.getController().setCenter(puntoGasto);
        map.getOverlays().clear();

        Marker marker = new Marker(map);
        marker.setPosition(puntoGasto);
        marker.setTitle(titulo != null && !titulo.isEmpty() ? titulo : "Ubicación del gasto");
        map.getOverlays().add(marker);

        if (titulo != null && !titulo.isEmpty()) {
            Toast.makeText(this, "📍 " + titulo, Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarTextoDistancia(float distancia) {
        if (distancia < 1000) {
            tvDistancia.setText("📍 Distancia actual: " + (int) distancia + " m");
        } else {
            tvDistancia.setText("📍 Distancia actual: " +
                    String.format(Locale.getDefault(), "%.2f", distancia / 1000f) + " km");
        }
    }

    private void configurarEstadoBoton(boolean activo) {
        if (activo) {
            btnSeguimiento.setText("Detener seguimiento");
            btnSeguimiento.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#DC2626"))
            );
        } else {
            btnSeguimiento.setText("Iniciar seguimiento");
            btnSeguimiento.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2563EB"))
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistrado) {
            registerReceiver(distanceReceiver, new IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE));
            receiverRegistrado = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistrado) {
            unregisterReceiver(distanceReceiver);
            receiverRegistrado = false;
        }
    }
}