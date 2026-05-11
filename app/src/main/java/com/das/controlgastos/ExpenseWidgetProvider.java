package com.das.controlgastos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.das.controlgastos.ui.LoginActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class ExpenseWidgetProvider extends AppWidgetProvider {

    private static final String BASE_URL = "https://gastos-api-495723811676.us-central1.run.app/login.php";

    public static void actualizarWidget(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId
    ) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_expense
        );

        SharedPreferences prefs = context.getSharedPreferences(
                "sesion",
                Context.MODE_PRIVATE
        );

        boolean logueado = prefs.getBoolean("logueado", false);
        String userEmail = prefs.getString("email", "");

        views.setTextViewText(
                R.id.tvWidgetTotal,
                "💰 Total: $0.00"
        );

        views.setTextViewText(
                R.id.tvWidgetCount,
                "📊 Registros: 0"
        );

        Intent intent;

        if (logueado && userEmail != null && !userEmail.isEmpty()) {
            intent = new Intent(context, MainActivity.class);

            cargarResumenDesdeServidor(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    userEmail
            );
        } else {
            intent = new Intent(context, LoginActivity.class);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(
                R.id.widgetContainer,
                pendingIntent
        );

        appWidgetManager.updateAppWidget(
                appWidgetId,
                views
        );
    }

    private static void cargarResumenDesdeServidor(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            String userEmail
    ) {
        new Thread(() -> {
            double total = 0;
            int cantidad = 0;

            try {
                URL url = new URL(BASE_URL + "resumen_gastos.php");

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

                JSONObject jsonObject = new JSONObject(result.toString());

                if (jsonObject.getBoolean("success")) {
                    total = jsonObject.getDouble("total");
                    cantidad = jsonObject.getInt("cantidad");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_expense
            );

            views.setTextViewText(
                    R.id.tvWidgetTotal,
                    "💰 Total: $" + String.format(Locale.getDefault(), "%.2f", total)
            );

            views.setTextViewText(
                    R.id.tvWidgetCount,
                    "📊 Registros: " + cantidad
            );

            Intent intent = new Intent(context, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            views.setOnClickPendingIntent(
                    R.id.widgetContainer,
                    pendingIntent
            );

            appWidgetManager.updateAppWidget(
                    appWidgetId,
                    views
            );
        }).start();
    }

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds
    ) {
        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            );
        }
    }

    public static void actualizarTodosLosWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName componentName = new ComponentName(
                context,
                ExpenseWidgetProvider.class
        );

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            );
        }
    }
}