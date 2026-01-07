package com.example.supportapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout; // Or the actual root type of item_location.xml
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations;
    private OnLocationClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnLocationClickListener {
        void onLocationClick(int position, Location location);
    }

    public LocationAdapter(List<Location> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false); // This will use res/layout-land/item_location.xml
        return new LocationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location currentLocation = locations.get(position);

        holder.tvLocationName.setText(currentLocation.getName());
        holder.tvSite.setText("Site: " + currentLocation.getSite());
        holder.tvLocationCode.setText("Code: " + currentLocation.getLocationCode());
        holder.tvQuantity.setText("Qty: " + String.valueOf(currentLocation.getQuantity()));

        if (selectedPosition == position) {
            holder.itemLayout.setBackgroundColor(Color.parseColor("#A0A0A0")); // A light grey for selection
        } else {
            // Check your item_location.xml's root background. If it's "?android:attr/selectableItemBackground",
            // setting to Color.TRANSPARENT is usually fine for the deselected state.
            // If item_location.xml has a specific background color, reset to that or Color.TRANSPARENT.
            holder.itemLayout.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (selectedPosition != holder.getAdapterPosition()) { // Check if position is valid
                    // Deselect old item
                    if (selectedPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(selectedPosition);
                    }
                    // Select new item
                    selectedPosition = holder.getAdapterPosition();
                    if (selectedPosition != RecyclerView.NO_POSITION) { // Ensure new position is valid
                        notifyItemChanged(selectedPosition);
                    }
                }
                // Call listener only if the new position is valid
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    listener.onLocationClick(selectedPosition, locations.get(selectedPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations == null ? 0 : locations.size();
    }

    public void setSelectedPosition(int position) {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition); // Deselect old
        }
        selectedPosition = position;
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition); // Select new
        }
    }

    public Location getSelectedItem() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < locations.size()) {
            return locations.get(selectedPosition);
        }
        return null;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvSite, tvLocationCode, tvQuantity;
        ConstraintLayout itemLayout; // Make sure this matches the root element type and ID in item_location.xml

        LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvSite = itemView.findViewById(R.id.tvSite);
            tvLocationCode = itemView.findViewById(R.id.tvLocationCode);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            itemLayout = itemView.findViewById(R.id.itemLocationLayout); // ID of the root in item_location.xml
        }
    }
}