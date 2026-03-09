package com.das.controlgastos.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.das.controlgastos.R;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotifications;
    private Spinner spinnerCurrency;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchNotifications = findViewById(R.id.switchNotifications);
        spinnerCurrency = findViewById(R.id.spinnerCurrency);

        preferences = getSharedPreferences("app_settings", MODE_PRIVATE);

        boolean notificationsEnabled = preferences.getBoolean("notifications", true);
        switchNotifications.setChecked(notificationsEnabled);

        String[] currencies = {"USD ($)", "EUR (€)"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                currencies
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);

        String savedCurrency = preferences.getString("currency", "$");
        if (savedCurrency.equals("$")) {
            spinnerCurrency.setSelection(0);
        } else {
            spinnerCurrency.setSelection(1);
        }

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("notifications", isChecked);
            editor.apply();
        });

        spinnerCurrency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String currency = position == 0 ? "$" : "€";
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("currency", currency);
                editor.apply();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }
}