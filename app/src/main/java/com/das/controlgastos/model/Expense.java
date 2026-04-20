package com.das.controlgastos.model;

public class Expense {

    private int id;
    private String title;
    private double amount;
    private String category;
    private String date;

    // Campos viejos
    private String description;
    private String paymentMethod;
    private boolean necessary;

    // Campos nuevos
    private double latitud;
    private double longitud;
    private String evidencia;

    // Constructor viejo
    public Expense(int id, String title, double amount, String category, String date,
                   String description, String paymentMethod, boolean necessary) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
        this.paymentMethod = paymentMethod;
        this.necessary = necessary;
        this.latitud = 0.0;
        this.longitud = 0.0;
        this.evidencia = "";
    }

    // Constructor con ubicación
    public Expense(int id, String title, double amount, String category, String date,
                   double latitud, double longitud) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.latitud = latitud;
        this.longitud = longitud;

        this.description = "";
        this.paymentMethod = "";
        this.necessary = false;
        this.evidencia = "";
    }

    // Constructor con ubicación + evidencia
    public Expense(int id, String title, double amount, String category, String date,
                   double latitud, double longitud, String evidencia) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.latitud = latitud;
        this.longitud = longitud;
        this.evidencia = evidencia;

        this.description = "";
        this.paymentMethod = "";
        this.necessary = false;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public boolean isNecessary() {
        return necessary;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public String getEvidencia() {
        return evidencia;
    }
}