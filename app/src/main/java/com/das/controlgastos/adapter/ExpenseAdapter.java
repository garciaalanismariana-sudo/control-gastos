package com.das.controlgastos.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.das.controlgastos.R;
import com.das.controlgastos.model.Expense;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final Context context;
    private final List<Expense> expenseList;
    private final OnExpenseClickListener clickListener;
    private final OnExpenseLongClickListener longClickListener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public interface OnExpenseLongClickListener {
        void onExpenseLongClick(Expense expense);
    }

    public ExpenseAdapter(Context context,
                          List<Expense> expenseList,
                          OnExpenseClickListener clickListener,
                          OnExpenseLongClickListener longClickListener) {
        this.context = context;
        this.expenseList = expenseList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        SharedPreferences preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String currency = preferences.getString("currency", "$");

        holder.tvTitle.setText(expense.getTitle());
        holder.tvAmount.setText(currency + String.format(Locale.getDefault(), "%.2f", expense.getAmount()));
        holder.tvDate.setText(expense.getDate());

        String category = expense.getCategory() != null
                ? expense.getCategory().toLowerCase(Locale.getDefault())
                : "";

        if (category.contains("aliment")) {
            holder.tvCategory.setText("🍔 " + expense.getCategory());
            holder.tvIcon.setText("🍔");
            holder.tvIcon.setTextColor(Color.parseColor("#16A34A"));
            holder.tvAmount.setTextColor(Color.parseColor("#16A34A"));
        } else if (category.contains("trans")) {
            holder.tvCategory.setText("🚗 " + expense.getCategory());
            holder.tvIcon.setText("🚗");
            holder.tvIcon.setTextColor(Color.parseColor("#2563EB"));
            holder.tvAmount.setTextColor(Color.parseColor("#2563EB"));
        } else if (category.contains("ocio")) {
            holder.tvCategory.setText("🎬 " + expense.getCategory());
            holder.tvIcon.setText("🎬");
            holder.tvIcon.setTextColor(Color.parseColor("#9333EA"));
            holder.tvAmount.setTextColor(Color.parseColor("#9333EA"));
        } else {
            holder.tvCategory.setText("💼 " + expense.getCategory());
            holder.tvIcon.setText("💰");
            holder.tvIcon.setTextColor(Color.parseColor("#14B8A6"));
            holder.tvAmount.setTextColor(Color.parseColor("#14B8A6"));
        }

        holder.itemView.setOnClickListener(v -> clickListener.onExpenseClick(expense));

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onExpenseLongClick(expense);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvAmount, tvCategory, tvDate, tvIcon;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvIcon = itemView.findViewById(R.id.tvIcon);
        }
    }
}