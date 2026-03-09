package com.das.controlgastos.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.das.controlgastos.R;
import com.das.controlgastos.database.DatabaseHelper;

import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {

    EditText etTitle, etAmount, etCategory, etDate;
    Button btnSaveExpense;
    DatabaseHelper databaseHelper;

    int expenseId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory);
        etDate = findViewById(R.id.etDate);
        btnSaveExpense = findViewById(R.id.btnSaveExpense);

        databaseHelper = new DatabaseHelper(this);

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

        if (getIntent() != null && getIntent().hasExtra("id")) {
            expenseId = getIntent().getIntExtra("id", -1);
            String title = getIntent().getStringExtra("title");
            double amount = getIntent().getDoubleExtra("amount", 0);
            String category = getIntent().getStringExtra("category");
            String date = getIntent().getStringExtra("date");

            etTitle.setText(title);
            etAmount.setText(String.valueOf(amount));
            etCategory.setText(category);
            etDate.setText(date);

            btnSaveExpense.setText(R.string.update_expense);
        }

        btnSaveExpense.setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();
            String amountText = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();

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

            double amount = Double.parseDouble(amountText);

            if (expenseId == -1) {
                databaseHelper.insertExpense(title, amount, category, date);
            } else {
                databaseHelper.updateExpense(expenseId, title, amount, category, date);
            }

            setResult(RESULT_OK);
            finish();
        });
    }
}