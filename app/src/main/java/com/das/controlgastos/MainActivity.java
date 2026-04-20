package com.das.controlgastos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.das.controlgastos.adapter.ExpenseAdapter;
import com.das.controlgastos.database.DatabaseHelper;
import com.das.controlgastos.model.Expense;
import com.das.controlgastos.ui.AddExpenseActivity;
import com.das.controlgastos.ui.LoginActivity;
import com.das.controlgastos.ui.SettingsActivity;
import com.das.controlgastos.utils.NotificationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewExpenses;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList;

    private FloatingActionButton fabAddExpense;
    private DatabaseHelper databaseHelper;

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
        databaseHelper = new DatabaseHelper(this);

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
                expense -> showDeleteDialog(expense)
        );

        recyclerViewExpenses.setAdapter(expenseAdapter);

        loadExpenses();

        fabAddExpense.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AddExpenseActivity.class), 1)
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

    }

    private void loadExpenses() {
        expenseList.clear();
        expenseList.addAll(databaseHelper.getAllExpenses(userEmail));
        expenseAdapter.notifyDataSetChanged();
        updateTotal();
        ExpenseWidgetProvider.actualizarTodosLosWidgets(this);
    }

    private void updateTotal() {
        SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String currencySymbol = preferences.getString("currency", "$");

        double total = 0;
        for (Expense e : expenseList) {
            total += e.getAmount();
        }

        tvTotal.setText(getString(R.string.total_spent_format, currencySymbol, total));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadExpenses();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage("¿Deseas eliminar este gasto?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    databaseHelper.deleteExpense(expense.getId(), userEmail);
                    loadExpenses();
                })
                .setNegativeButton("No", null)
                .show();
    }
}