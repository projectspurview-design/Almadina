package com.example.Pickbyvision.Induvidual_Pick.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Pickbyvision.Induvidual_Pick.Location.Location;
import com.example.Pickbyvision.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {
    private static final String TAG = "LocationAdapter";

    private List<Location> locations;
    private OnLocationClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    private OkHttpClient httpClient;
    private ExecutorService imageLoadExecutor;
    private Handler mainHandler;

    public interface OnLocationClickListener {
        void onLocationClick(int position, Location location);
    }

    public LocationAdapter(List<Location> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;

        this.httpClient = new OkHttpClient();
        this.imageLoadExecutor = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
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
            holder.itemLayout.setBackgroundColor(Color.parseColor("#A0A0A0"));
        } else {
            holder.itemLayout.setBackgroundColor(Color.TRANSPARENT);
        }


        if (holder.ivProductImage != null) {
            loadImageFromAws(holder.ivProductImage, currentLocation.getAwsPath());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (selectedPosition != holder.getAdapterPosition()) {
                    if (selectedPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(selectedPosition);
                    }
                    selectedPosition = holder.getAdapterPosition();
                    if (selectedPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(selectedPosition);
                    }
                }
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    listener.onLocationClick(selectedPosition, locations.get(selectedPosition));
                }
            }
        });
    }


    private void loadImageFromAws(ImageView imageView, String awsPath) {

        imageView.setImageResource(android.R.drawable.ic_menu_gallery);

        if (awsPath == null || awsPath.trim().isEmpty()) {
            Log.d(TAG, "No AWS path provided for image");
            return;
        }


        imageLoadExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(awsPath)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Failed to load image from: " + awsPath, e);

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try (InputStream inputStream = response.body().byteStream()) {
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                if (bitmap != null) {

                                    mainHandler.post(() -> {
                                        imageView.setImageBitmap(bitmap);
                                        Log.d(TAG, "Successfully loaded image from: " + awsPath);
                                    });
                                } else {
                                    Log.e(TAG, "Failed to decode bitmap from: " + awsPath);
                                }
                            }
                        } else {
                            Log.e(TAG, "HTTP error loading image: " + response.code() + " from: " + awsPath);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating image request for: " + awsPath, e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations == null ? 0 : locations.size();
    }

    public void setSelectedPosition(int position) {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
        }
        selectedPosition = position;
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
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

    public void cleanup() {
        if (imageLoadExecutor != null && !imageLoadExecutor.isShutdown()) {
            imageLoadExecutor.shutdown();
        }
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvSite, tvLocationCode, tvQuantity;
        ConstraintLayout itemLayout;
        ImageView ivProductImage;

        LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvSite = itemView.findViewById(R.id.tvSite);
            tvLocationCode = itemView.findViewById(R.id.tvLocationCode);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            itemLayout = itemView.findViewById(R.id.itemLocationLayout);


            try {
                ivProductImage = itemView.findViewById(R.id.ivProductImage);
            } catch (Exception e) {
                Log.d(TAG, "ImageView not found in layout, continuing without image support");
                ivProductImage = null;
            }
        }
    }
}