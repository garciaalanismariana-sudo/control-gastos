package com.das.controlgastos.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.das.controlgastos.R;
import com.das.controlgastos.database.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etTitle, etAmount, etCategory, etDate;
    private Button btnSaveExpense, btnUbicacion, btnVerMapa, btnCapturarEvidencia;
    private TextView tvUbicacionEstado, tvEvidenciaEstado;
    private ImageView ivEvidencia;

    private DatabaseHelper databaseHelper;

    private int expenseId = -1;

    private double latitud = 0.0;
    private double longitud = 0.0;

    private FusedLocationProviderClient fusedLocationClient;

    private Uri evidenciaUri = null;
    private String evidenciaPathLocal = "";
    private String evidenciaServidor = "";

    private ActivityResultLauncher<Intent> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory);
        etDate = findViewById(R.id.etDate);
        btnSaveExpense = findViewById(R.id.btnSaveExpense);
        btnUbicacion = findViewById(R.id.btnUbicacion);
        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnCapturarEvidencia = findViewById(R.id.btnCapturarEvidencia);
        tvUbicacionEstado = findViewById(R.id.tvUbicacionEstado);
        tvEvidenciaEstado = findViewById(R.id.tvEvidenciaEstado);
        ivEvidencia = findViewById(R.id.ivEvidencia);

        databaseHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && evidenciaUri != null) {
                        ivEvidencia.setImageURI(evidenciaUri);
                        ivEvidencia.setVisibility(View.VISIBLE);
                        tvEvidenciaEstado.setText("📷 Evidencia capturada");
                        btnCapturarEvidencia.setText("Actualizar evidencia");
                    } else {
                        Toast.makeText(this, "No se capturó evidencia", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddExpenseActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etDate.setText(formattedDate);
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });

        btnUbicacion.setOnClickListener(v -> obtenerUbicacionActual());

        btnVerMapa.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, MapActivity.class);
            intent.putExtra("latitud", latitud);
            intent.putExtra("longitud", longitud);
            intent.putExtra("titulo", etTitle.getText().toString().trim());
            startActivity(intent);
        });

        btnCapturarEvidencia.setOnClickListener(v -> capturarEvidencia());

        if (getIntent() != null && getIntent().hasExtra("id")) {
            expenseId = getIntent().getIntExtra("id", -1);
            String title = getIntent().getStringExtra("title");
            double amount = getIntent().getDoubleExtra("amount", 0);
            String category = getIntent().getStringExtra("category");
            String date = getIntent().getStringExtra("date");

            latitud = getIntent().getDoubleExtra("latitud", 0.0);
            longitud = getIntent().getDoubleExtra("longitud", 0.0);
            evidenciaServidor = getIntent().getStringExtra("evidencia");

            etTitle.setText(title);
            etAmount.setText(String.valueOf(amount));
            etCategory.setText(category);
            etDate.setText(date);

            if (evidenciaServidor != null && !evidenciaServidor.isEmpty()) {
                cargarEvidenciaDesdeServidor(evidenciaServidor);
            }

            btnSaveExpense.setText("Actualizar gasto");
        }

        actualizarEstadoUbicacion();
        actualizarEstadoEvidencia();

        btnSaveExpense.setOnClickListener(v -> guardarGasto());
    }

    private void guardarGasto() {
        String title = etTitle.getText().toString().trim();
        String amountText = etAmount.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        String userEmail = prefs.getString("email", "");

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            etTitle.setError("Ingresa el nombre del gasto");
            etTitle.requestFocus();
            return;
        }

        if (amountText.isEmpty()) {
            etAmount.setError("Ingresa el monto");
            etAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            etCategory.setError("Ingresa la categoría");
            etCategory.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Ingresa la fecha");
            etDate.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etAmount.setError("Ingresa un monto válido");
            etAmount.requestFocus();
            return;
        }

        double finalAmount = amount;

        new Thread(() -> {
            String evidenciaFinal = evidenciaServidor;

            if (evidenciaPathLocal != null && !evidenciaPathLocal.isEmpty()) {
                String rutaSubida = subirEvidenciaAlServidor(evidenciaPathLocal);
                if (rutaSubida != null && !rutaSubida.equals("error")) {
                    evidenciaFinal = rutaSubida;
                }
            }

            String finalEvidencia = evidenciaFinal;

            runOnUiThread(() -> {
                if (expenseId == -1) {
                    databaseHelper.insertExpense(title, finalAmount, category, date, latitud, longitud, finalEvidencia, userEmail);
                    Toast.makeText(AddExpenseActivity.this, "Gasto guardado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    databaseHelper.updateExpense(expenseId, title, finalAmount, category, date, latitud, longitud, finalEvidencia, userEmail);
                    Toast.makeText(AddExpenseActivity.this, "Gasto actualizado correctamente", Toast.LENGTH_SHORT).show();
                }

                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }

    private String subirEvidenciaAlServidor(String pathLocal) {
        try {
            File file = new File(pathLocal);
            if (!file.exists()) return "error";

            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            byte[] bytes = stream.toByteArray();
            String imagenBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

            String nombreArchivo = file.getName();

            URL url = new URL("http://10.0.2.2:8080/controlgastos/subir_evidencia.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = new android.net.Uri.Builder()
                    .appendQueryParameter("imagen", imagenBase64)
                    .appendQueryParameter("nombre", nombreArchivo)
                    .build()
                    .getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            reader.close();
            conn.disconnect();

            return resultBuilder.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    private void cargarEvidenciaDesdeServidor(String rutaServidor) {
        new Thread(() -> {
            try {
                String urlCompleta = "http://10.0.2.2:8080/controlgastos/" + rutaServidor;

                URL url = new URL(urlCompleta);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());

                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            ivEvidencia.setImageBitmap(bitmap);
                            ivEvidencia.setVisibility(View.VISIBLE);
                            tvEvidenciaEstado.setText("📷 Evidencia guardada");
                            btnCapturarEvidencia.setText("Actualizar evidencia");
                        }
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitud = location.getLatitude();
                        longitud = location.getLongitude();

                        actualizarEstadoUbicacion();

                        Toast.makeText(
                                AddExpenseActivity.this,
                                "Ubicación guardada: " + latitud + ", " + longitud,
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                AddExpenseActivity.this,
                                "No se pudo obtener ubicación. Intenta otra vez.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        AddExpenseActivity.this,
                        "Error obteniendo ubicación",
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void capturarEvidencia() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    200
            );
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String nombreArchivo = "EVIDENCIA_" + timeStamp + "_";

            File directorio = getExternalFilesDir(null);
            File archivoImagen = File.createTempFile(nombreArchivo, ".jpg", directorio);

            evidenciaPathLocal = archivoImagen.getAbsolutePath();

            evidenciaUri = FileProvider.getUriForFile(
                    this,
                    "com.das.controlgastos.provider",
                    archivoImagen
            );

            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, evidenciaUri);
            takePictureLauncher.launch(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparando la evidencia", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarEstadoUbicacion() {
        boolean tieneUbicacion = latitud != 0.0 || longitud != 0.0;

        if (tieneUbicacion) {
            tvUbicacionEstado.setText("📍 Ubicación guardada");
            btnUbicacion.setText("Actualizar ubicación actual");
            btnVerMapa.setVisibility(View.VISIBLE);
        } else {
            tvUbicacionEstado.setText("📍 Sin ubicación guardada");
            btnUbicacion.setText("Guardar ubicación actual");
            btnVerMapa.setVisibility(View.GONE);
        }
    }

    private void actualizarEstadoEvidencia() {
        boolean tieneEvidenciaLocal = evidenciaPathLocal != null && !evidenciaPathLocal.isEmpty();
        boolean tieneEvidenciaServidor = evidenciaServidor != null && !evidenciaServidor.isEmpty();

        if (tieneEvidenciaLocal) {
            tvEvidenciaEstado.setText("📷 Evidencia capturada");
            btnCapturarEvidencia.setText("Actualizar evidencia");
            ivEvidencia.setVisibility(View.VISIBLE);
        } else if (tieneEvidenciaServidor) {
            tvEvidenciaEstado.setText("📷 Evidencia guardada");
            btnCapturarEvidencia.setText("Actualizar evidencia");
            ivEvidencia.setVisibility(View.VISIBLE);
        } else {
            tvEvidenciaEstado.setText("📷 Sin evidencia adjunta");
            btnCapturarEvidencia.setText("Capturar evidencia");
            ivEvidencia.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturarEvidencia();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}