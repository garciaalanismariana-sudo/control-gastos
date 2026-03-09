package com.das.controlgastos.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gastos.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createTable = "CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "amount REAL," +
                "category TEXT," +
                "date TEXT" +
                ")";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS expenses");
        onCreate(db);

    }

    public void insertExpense(String title, double amount, String category, String date){

        SQLiteDatabase db = this.getWritableDatabase();

        String query = "INSERT INTO expenses (title, amount, category, date) VALUES ('"
                + title + "',"
                + amount + ",'"
                + category + "','"
                + date + "')";

        db.execSQL(query);
    }

    public Cursor getAllExpenses(){

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT * FROM expenses ORDER BY id DESC", null);
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM expenses WHERE id = " + id);
    }

    public void updateExpense(int id, String title, double amount, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "UPDATE expenses SET " +
                "title = '" + title + "', " +
                "amount = " + amount + ", " +
                "category = '" + category + "', " +
                "date = '" + date + "' " +
                "WHERE id = " + id;

        db.execSQL(query);
    }
}