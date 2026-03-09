package com.das.controlgastos.model;

public class Expense {

    private int id;
    private String title;
    private double amount;
    private String category;
    private String date;
    private String description;
    private String paymentMethod;
    private boolean necessary;

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

}