package com.das.controlgastos.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.das.controlgastos.model.Expense;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gastos.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_EXPENSES = "expenses";

    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DATE = "date";
    private static final String COL_LATITUD = "latitud";
    private static final String COL_LONGITUD = "longitud";
    private static final String COL_EVIDENCIA = "evidencia";
    private static final String COL_USER_EMAIL = "user_email";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_LATITUD + " REAL, " +
                COL_LONGITUD + " REAL, " +
                COL_EVIDENCIA + " TEXT, " +
                "user_email TEXT" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }

    public void insertExpense(String title, double amount, String category, String date,
                              double latitud, double longitud, String evidencia, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_TITLE, title);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        values.put(COL_LATITUD, latitud);
        values.put(COL_LONGITUD, longitud);
        values.put(COL_EVIDENCIA, evidencia);
        values.put(COL_USER_EMAIL, userEmail);

        db.insert(TABLE_EXPENSES, null, values);
        db.close();
    }

    public void updateExpense(int id, String title, double amount, String category, String date,
                              double latitud, double longitud, String evidencia, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_TITLE, title);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        values.put(COL_LATITUD, latitud);
        values.put(COL_LONGITUD, longitud);
        values.put(COL_EVIDENCIA, evidencia);
        values.put(COL_USER_EMAIL, userEmail);

        db.update(
                TABLE_EXPENSES,
                values,
                COL_ID + "=? AND " + COL_USER_EMAIL + "=?",
                new String[]{String.valueOf(id), userEmail}
        );
        db.close();
    }

    public List<Expense> getAllExpenses(String userEmail) {
        List<Expense> expenseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES + " WHERE " + COL_USER_EMAIL + " = ? ORDER BY " + COL_ID + " DESC",
                new String[]{userEmail}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                double latitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUD));
                double longitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUD));
                String evidencia = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVIDENCIA));

                Expense expense = new Expense(id, title, amount, category, date, latitud, longitud, evidencia);
                expenseList.add(expense);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return expenseList;
    }

    public void deleteExpense(int id, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(
                TABLE_EXPENSES,
                COL_ID + "=? AND " + COL_USER_EMAIL + "=?",
                new String[]{String.valueOf(id), userEmail}
        );
        db.close();
    }

    public double getTotalExpensesAmount(String userEmail) {
        double total = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES + " WHERE " + COL_USER_EMAIL + " = ?",
                new String[]{userEmail}
        );

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        db.close();
        return total;
    }

    public int getExpensesCount(String userEmail) {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_EXPENSES + " WHERE " + COL_USER_EMAIL + " = ?",
                new String[]{userEmail}
        );

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}