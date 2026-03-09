package com.das.controlgastos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.das.controlgastos.adapter.ExpenseAdapter;
import com.das.controlgastos.database.DatabaseHelper;
import com.das.controlgastos.model.Expense;
import com.das.controlgastos.ui.AddExpenseActivity;
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
    private Button btnNotification;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createChannel(this);

        databaseHelper = new DatabaseHelper(this);

        recyclerViewExpenses = findViewById(R.id.recyclerViewExpenses);
        recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(this));

        fabAddExpense = findViewById(R.id.fabAddExpense);
        tvTotal = findViewById(R.id.tvTotal);
        btnNotification = findViewById(R.id.btnNotification);
        btnSettings = findViewById(R.id.btnSettings);

        expenseList = new ArrayList<>();

        expenseAdapter = new ExpenseAdapter(
                this,
                expenseList,
                expense -> {
                    Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
                    intent.putExtra("id", expense.getId());
                    intent.putExtra("title", expense.getTitle());
                    intent.putExtra("amount", expense.getAmount());
                    intent.putExtra("category", expense.getCategory());
                    intent.putExtra("date", expense.getDate());
                    startActivityForResult(intent, 1);
                },
                expense -> showDeleteDialog(expense)
        );

        recyclerViewExpenses.setAdapter(expenseAdapter);

        loadExpenses();

        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivityForResult(intent, 1);
        });

        btnNotification.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
            boolean notificationsEnabled = preferences.getBoolean("notifications", true);

            if (notificationsEnabled) {
                NotificationHelper.showNotification(this);
            }
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadExpenses() {

        expenseList.clear();

        Cursor cursor = databaseHelper.getAllExpenses();

        if (cursor != null && cursor.moveToFirst()) {

            do {

                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                double amount = cursor.getDouble(2);
                String category = cursor.getString(3);
                String date = cursor.getString(4);

                Expense expense = new Expense(
                        id,
                        title,
                        amount,
                        category,
                        date,
                        "",
                        "",
                        true
                );

                expenseList.add(expense);

            } while (cursor.moveToNext());

            cursor.close();
        }

        expenseAdapter.notifyDataSetChanged();
        updateTotal();
    }

    private void updateTotal() {

        double total = 0;

        for (Expense expense : expenseList) {
            total += expense.getAmount();
        }

        SharedPreferences preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String currency = preferences.getString("currency", "$");

        tvTotal.setText("Total gastado: " + currency + String.format("%.2f", total));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTotal();
        expenseAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadExpenses();
        }
    }

    private void showDeleteDialog(Expense expense) {

        new AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage("¿Deseas eliminar este gasto?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    databaseHelper.deleteExpense(expense.getId());
                    loadExpenses();
                })
                .setNegativeButton("No", null)
                .show();
    }
}