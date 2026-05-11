package com.das.controlgastos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.das.controlgastos.MainActivity;
import com.das.controlgastos.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://gastos-api-495723811676.us-central1.run.app/";

    EditText email, password;
    Button btnLogin;
    Button btnIrRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnIrRegistro = findViewById(R.id.btnIrRegistro);

        btnLogin.setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(
                        this,
                        "Completa todos los campos",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            new Thread(() -> loginUsuario(e, p)).start();
        });

        btnIrRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginUsuario(String emailTxt, String passwordTxt) {

        try {

            URL url = new URL(BASE_URL + "login.php");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            );

            String params = new android.net.Uri.Builder()
                    .appendQueryParameter("email", emailTxt.trim())
                    .appendQueryParameter("password", passwordTxt.trim())
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

                try {

                    JSONObject json = new JSONObject(result);

                    boolean success = json.getBoolean("success");

                    if (!success) {

                        Toast.makeText(
                                LoginActivity.this,
                                json.optString("message", "Login incorrecto"),
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String nombreUsuario = json.getString("nombre");
                    String emailUsuario = json.getString("email");

                    getSharedPreferences("sesion", MODE_PRIVATE)
                            .edit()
                            .putBoolean("logueado", true)
                            .putString("nombre", nombreUsuario)
                            .putString("email", emailUsuario)
                            .apply();

                    Toast.makeText(
                            LoginActivity.this,
                            "Bienvenido " + nombreUsuario,
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            LoginActivity.this,
                            MainActivity.class
                    );

                    startActivity(intent);
                    finish();

                } catch (Exception ex) {

                    Toast.makeText(
                            LoginActivity.this,
                            "Respuesta: " + result,
                            Toast.LENGTH_LONG
                    ).show();

                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {

            e.printStackTrace();

            runOnUiThread(() ->
                    Toast.makeText(
                            LoginActivity.this,
                            "Error conectando servidor",
                            Toast.LENGTH_LONG
                    ).show()
            );
        }
    }
}