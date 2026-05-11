package com.das.controlgastos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.das.controlgastos.adapter.ExpenseAdapter;
import com.das.controlgastos.model.Expense;
import com.das.controlgastos.ui.AddExpenseActivity;
import com.das.controlgastos.ui.LoginActivity;
import com.das.controlgastos.ui.SettingsActivity;
import com.das.controlgastos.utils.NotificationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://gastos-api-495723811676.us-central1.run.app/";
    private RecyclerView recyclerViewExpenses;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList;

    private FloatingActionButton fabAddExpense;

    private TextView tvTotal;
    private Button btnNotification, btnSettings;
    private ImageButton btnLogout;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
        boolean logueado = prefs.getBoolean("logueado", false);
        userEmail = prefs.getString("email", "");

        if (!logueado || userEmail == null || userEmail.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        NotificationHelper.createChannel(this);

        recyclerViewExpenses = findViewById(R.id.recyclerViewExpenses);
        recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(this));

        fabAddExpense = findViewById(R.id.fabAddExpense);
        tvTotal = findViewById(R.id.tvTotal);
        btnNotification = findViewById(R.id.btnNotification);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);

        expenseList = new ArrayList<>();

        expenseAdapter = new ExpenseAdapter(
                this,
                expenseList,
                expense -> {
                    Intent intent = new Intent(this, AddExpenseActivity.class);
                    intent.putExtra("id", expense.getId());
                    intent.putExtra("title", expense.getTitle());
                    intent.putExtra("amount", expense.getAmount());
                    intent.putExtra("category", expense.getCategory());
                    intent.putExtra("date", expense.getDate());
                    intent.putExtra("latitud", expense.getLatitud());
                    intent.putExtra("longitud", expense.getLongitud());
                    intent.putExtra("evidencia", expense.getEvidencia());
                    startActivityForResult(intent, 1);
                },
                this::showDeleteDialog
        );

        recyclerViewExpenses.setAdapter(expenseAdapter);

        fabAddExpense.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, AddExpenseActivity.class),
                        1
                )
        );

        btnNotification.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);

            if (preferences.getBoolean("notifications", true)) {
                NotificationHelper.showNotification(this);
            }
        });

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Deseas cerrar sesión?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            getSharedPreferences("sesion", MODE_PRIVATE)
                                    .edit()
                                    .clear()
                                    .apply();

                            ExpenseWidgetProvider.actualizarTodosLosWidgets(this);

                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        loadExpenses();
    }

    private void loadExpenses() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "get_expenses.php");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );

                String params =
                        "user_email=" + java.net.URLEncoder.encode(userEmail, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(params.getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                System.out.println("RESPUESTA GET_EXPENSES: " + result.toString());

                org.json.JSONObject json = new org.json.JSONObject(result.toString());
                if (json.getBoolean("success")) {
                    JSONArray array = json.getJSONArray("expenses");
                    List<Expense> listaServidor = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);

                        Expense expense = new Expense(
                                obj.getInt("id"),
                                obj.getString("title"),
                                obj.getDouble("amount"),
                                obj.getString("category"),
                                obj.getString("date"),
                                obj.optDouble("latitud", 0.0),
                                obj.optDouble("longitud", 0.0),
                                obj.optString("evidencia", "")
                        );

                        listaServidor.add(expense);
                    }

                    runOnUiThread(() -> {
                        expenseList.clear();
                        expenseList.addAll(listaServidor);
                        expenseAdapter.notifyDataSetChanged();
                        updateTotal();
                        ExpenseWidgetProvider.actualizarTodosLosWidgets(this);
                    });
                } else {
                    String message = json.optString("message", "Error cargando gastos");

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    private void updateTotal() {
        SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String currencySymbol = preferences.getString("currency", "$");

        double total = 0;

        for (Expense e : expenseList) {
            total += e.getAmount();
        }

        tvTotal.setText(
                getString(
                        R.string.total_spent_format,
                        currencySymbol,
                        total
                )
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userEmail != null && !userEmail.isEmpty()) {
            loadExpenses();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadExpenses();
        }
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage("¿Deseas eliminar este gasto?")
                .setPositiveButton("Sí", (dialog, which) ->
                        eliminarGastoServidor(expense.getId())
                )
                .setNegativeButton("No", null)
                .show();
    }

    private void eliminarGastoServidor(int id) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "delete_expense.php");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );

                String params =
                        "id=" + java.net.URLEncoder.encode(String.valueOf(id), "UTF-8") +
                                "&user_email=" + java.net.URLEncoder.encode(userEmail, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(params.getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(result.toString());

                runOnUiThread(() -> {
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(
                                MainActivity.this,
                                "Gasto eliminado",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadExpenses();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                json.optString("message", "No se pudo eliminar"),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Error eliminando gasto",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        }).start();
    }
}