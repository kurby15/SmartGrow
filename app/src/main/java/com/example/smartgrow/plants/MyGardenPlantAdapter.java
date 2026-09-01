package com.example.smartgrow.plants;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;

import java.util.List;

public class MyGardenPlantAdapter extends RecyclerView.Adapter<MyGardenPlantAdapter.PlantViewHolder> {

    private final List<MyGardenPlantModel> plantList;
    private OnPlantClickListener listener;

    // 🔴 Updated Interface to include "Rename" / "Name My Plant" and "Remove"
    public interface OnPlantClickListener {
        void onAddReminderClick(MyGardenPlantModel plant);
        void onRenamePlantClick(MyGardenPlantModel plant);
        void onRemovePlantClick(MyGardenPlantModel plant);
    }

    public MyGardenPlantAdapter(List<MyGardenPlantModel> plantList, OnPlantClickListener listener) {
        this.plantList = plantList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_garden_plant_card, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        MyGardenPlantModel plant = plantList.get(position);

        // Common Name / Plant Name
        holder.tvCommonName.setText(plant.getPlantName() != null ? plant.getPlantName() : "Unknown Plant");

        // Scientific Name (fallback if null)
        if (plant.getScientificName() != null && !plant.getScientificName().isEmpty()) {
            holder.tvScientificName.setText(plant.getScientificName());
            holder.tvScientificName.setVisibility(View.VISIBLE);
        } else {
            holder.tvScientificName.setVisibility(View.GONE);
        }

        // Health Status & Percentage
        holder.tvHealthStatus.setText(plant.getHealthStatus() != null ? plant.getHealthStatus() : "N/A");
        holder.tvHealthPercentage.setText(plant.getHealthPercentage() + "%");

        // Load Image from Base64 String or Fallback
        if (plant.getImageBase64() != null && !plant.getImageBase64().isEmpty()) {
            Bitmap decodedBitmap = decodeBase64ToBitmap(plant.getImageBase64());
            if (decodedBitmap != null) {
                holder.ivPlantImage.setImageBitmap(decodedBitmap);
            } else {
                holder.ivPlantImage.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.ivPlantImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Dynamic Color for Health Status
        if (plant.getHealthPercentage() >= 80) {
            holder.tvHealthStatus.setTextColor(Color.parseColor("#2ECC71")); // Green
            holder.tvHealthPercentage.setTextColor(Color.parseColor("#2ECC71"));
        } else if (plant.getHealthPercentage() >= 50) {
            holder.tvHealthStatus.setTextColor(Color.parseColor("#F39C12")); // Orange
            holder.tvHealthPercentage.setTextColor(Color.parseColor("#F39C12"));
        } else {
            holder.tvHealthStatus.setTextColor(Color.parseColor("#E74C3C")); // Red
            holder.tvHealthPercentage.setTextColor(Color.parseColor("#E74C3C"));
        }

        // Reminder Click
        holder.btnAddReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddReminderClick(plant);
            }
        });

        // More Options Click (Card Level Popup Menu)
        if (holder.ibMoreOptions != null) {
            holder.ibMoreOptions.setOnClickListener(v -> showPopupMenu(v, plant));
        }
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    /**
     * Helper method to convert Base64 string from Firestore into an Android Bitmap
     */
    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Card Popup Menu ("Move", "Name My Plant", "Add Notes", "Remove")
     */
    private void showPopupMenu(View view, MyGardenPlantModel plant) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.plant_more_options_menu);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            String plantName = plant.getPlantName() != null ? plant.getPlantName() : "Plant";

            if (itemId == R.id.action_move) {
                Toast.makeText(view.getContext(), "Move " + plantName, Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.action_name_plant) {
                // 🔴 FIXED: Delegate to listener to trigger BottomSheet in Fragment instead of Toast!
                if (listener != null) {
                    listener.onRenamePlantClick(plant);
                }
                return true;
            } else if (itemId == R.id.action_add_notes) {
                Toast.makeText(view.getContext(), "Add Notes for " + plantName, Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.action_remove) {
                if (listener != null) {
                    listener.onRemovePlantClick(plant);
                }
                return true;
            }

            return false;
        });

        popup.show();
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlantImage;
        TextView tvCommonName, tvScientificName, tvHealthStatus, tvHealthPercentage;
        View btnAddReminder;
        ImageButton ibMoreOptions;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlantImage = itemView.findViewById(R.id.iv_plant_image);
            tvCommonName = itemView.findViewById(R.id.tv_common_name);
            tvScientificName = itemView.findViewById(R.id.tv_scientific_name);
            tvHealthStatus = itemView.findViewById(R.id.tv_health_status);
            tvHealthPercentage = itemView.findViewById(R.id.tv_health_percentage);
            btnAddReminder = itemView.findViewById(R.id.btn_add_reminder);
            ibMoreOptions = itemView.findViewById(R.id.ib_more_options);
        }
    }
}