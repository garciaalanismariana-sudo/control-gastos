package com.das.controlgastos.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.das.controlgastos.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    EditText nombre, email, password;
    Button btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nombre = findViewById(R.id.etNombre);
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> {
            String n = nombre.getText().toString().trim();
            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> registrarUsuario(n, e, p)).start();
        });
    }

    private void registrarUsuario(String nombreTxt, String emailTxt, String passwordTxt) {
        try {
            URL url = new URL("https://mariana.alwaysdata.net/registro_usuario.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Referer", "https://mariana.alwaysdata.net/");
            conn.setRequestProperty("Origin", "https://mariana.alwaysdata.net");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = new android.net.Uri.Builder()
                    .appendQueryParameter("nombre", nombreTxt)
                    .appendQueryParameter("email", emailTxt)
                    .appendQueryParameter("password", passwordTxt)
                    .build()
                    .getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream()
                    )
            );

            StringBuilder resultBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            reader.close();
            conn.disconnect();

            String result = resultBuilder.toString().trim();

            runOnUiThread(() -> {
                if (result.equals("ok")) {
                    Toast.makeText(RegisterActivity.this, "Registro correcto", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (result.equals("existe")) {
                    Toast.makeText(RegisterActivity.this, "El email ya está registrado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error al registrar", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(RegisterActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show()
            );
        }
    }
}