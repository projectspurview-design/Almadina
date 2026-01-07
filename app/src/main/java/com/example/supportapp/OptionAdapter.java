package com.example.supportapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {
    private List<String> options;
    private int selectedPosition = -1;

    public OptionAdapter(List<String> options) {
        this.options = options;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged(); // Notify all items to update
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String option = options.get(position);
        holder.textView.setText(option);

        if (position == selectedPosition) {
            // Apply selected style
            holder.itemView.setBackgroundResource(R.drawable.selected_border);
            holder.textView.setTextColor(Color.WHITE);
        } else {
            // Apply faded style
            holder.itemView.setBackgroundResource(0); // No border
            holder.textView.setTextColor(0x88FFFFFF); // Semi-transparent white
        }
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.optionText);
        }
    }
}