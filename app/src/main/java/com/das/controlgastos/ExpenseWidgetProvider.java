package com.das.controlgastos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.das.controlgastos.database.DatabaseHelper;
import com.das.controlgastos.ui.LoginActivity;

import java.util.Locale;

public class ExpenseWidgetProvider extends AppWidgetProvider {

    public static void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_expense);

        SharedPreferences prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        boolean logueado = prefs.getBoolean("logueado", false);
        String userEmail = prefs.getString("email", "");

        double total = 0;
        int cantidad = 0;

        if (logueado && userEmail != null && !userEmail.isEmpty()) {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            total = databaseHelper.getTotalExpensesAmount(userEmail);
            cantidad = databaseHelper.getExpensesCount(userEmail);
        }

        views.setTextViewText(
                R.id.tvWidgetTotal,
                "💰 Total: $" + String.format(Locale.getDefault(), "%.2f", total)
        );

        views.setTextViewText(
                R.id.tvWidgetCount,
                "📊 Registros: " + cantidad
        );

        Intent intent;
        if (logueado && userEmail != null && !userEmail.isEmpty()) {
            intent = new Intent(context, MainActivity.class);
        } else {
            intent = new Intent(context, LoginActivity.class);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void actualizarTodosLosWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ExpenseWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId);
        }
    }
}