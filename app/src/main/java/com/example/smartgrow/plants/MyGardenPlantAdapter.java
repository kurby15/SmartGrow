package com.example.smartgrow.plants;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyGardenPlantAdapter extends RecyclerView.Adapter<MyGardenPlantAdapter.PlantViewHolder> {

    private final List<MyGardenPlantModel> plantList;
    private OnPlantClickListener listener;

    public interface OnPlantClickListener {
        void onAddReminderClick(MyGardenPlantModel plant);
        void onPlantClick(MyGardenPlantModel plant);
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

        holder.tvCommonName.setText(plant.getPlantName() != null ? plant.getPlantName() : "Unknown Plant");

        if (plant.getScientificName() != null && !plant.getScientificName().isEmpty()) {
            holder.tvScientificName.setText(plant.getScientificName());
            holder.tvScientificName.setVisibility(View.VISIBLE);
        } else {
            holder.tvScientificName.setVisibility(View.GONE);
        }

        // Logic for "Needs Water" status display on card
        String healthText = plant.getHealthStatus() != null ? plant.getHealthStatus() : "N/A";
        boolean needsWater = checkIfNeedsWater(plant);
        
        if (needsWater) {
            holder.tvHealthStatus.setText("💧 Need Water");
            holder.tvHealthStatus.setTextColor(Color.parseColor("#3498DB")); // Blue
            holder.tvHealthPercentage.setVisibility(View.GONE);
        } else {
            holder.tvHealthStatus.setText(healthText);
            holder.tvHealthPercentage.setText(plant.getHealthPercentage() + "%");
            holder.tvHealthPercentage.setVisibility(View.VISIBLE);
            
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
        }

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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlantClick(plantList.get(position));
            }
        });

        holder.btnAddReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddReminderClick(plant);
            }
        });

        if (holder.ibMoreOptions != null) {
            holder.ibMoreOptions.setOnClickListener(v -> showPopupMenu(v, plant));
        }
    }

    private boolean checkIfNeedsWater(MyGardenPlantModel p) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        boolean isWateredToday = todayDate.equals(p.getLastWateredDate());
        if (isWateredToday) return false;

        Map<String, Object> reminders = p.getReminders();
        if (reminders != null) {
            String waterFreq = (String) reminders.get("wateringSchedule");
            String prefTime = (String) reminders.get("preferredTime");

            if (waterFreq != null && !"None".equalsIgnoreCase(waterFreq)) {
                if (isTaskDue(waterFreq, p.getLastWateredDate())) {
                    return isTimeReached(prefTime);
                }
            } else if (p.getHealthPercentage() < 60) {
                return true;
            }
        } else if (p.getHealthPercentage() < 60) {
            return true;
        }
        return false;
    }

    private boolean isTimeReached(String prefTime) {
        if (prefTime == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date timeDate = sdf.parse(prefTime);
            if (timeDate == null) return true;

            Calendar schedCal = Calendar.getInstance();
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(timeDate);

            schedCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            schedCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            schedCal.set(Calendar.SECOND, 0);
            schedCal.set(Calendar.MILLISECOND, 0);

            Calendar currentCal = Calendar.getInstance();
            return !currentCal.before(schedCal);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTaskDue(String frequency, String lastDate) {
        if (frequency == null || "None".equalsIgnoreCase(frequency)) return false;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (todayDate.equals(lastDate)) return true;
        if (lastDate == null || lastDate.isEmpty()) return true;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date last = sdf.parse(lastDate);
            Date today = sdf.parse(todayDate);

            long diffInMillis = Math.abs(today.getTime() - last.getTime());
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

            if ("Every Day".equalsIgnoreCase(frequency)) return diffInDays >= 1;
            if ("Every 2 Days".equalsIgnoreCase(frequency)) return diffInDays >= 2;
            if ("Every 3 Days".equalsIgnoreCase(frequency)) return diffInDays >= 3;
            if ("Weekly".equalsIgnoreCase(frequency) || "Every Week".equalsIgnoreCase(frequency)) return diffInDays >= 7;
            if ("Every 2 Weeks".equalsIgnoreCase(frequency)) return diffInDays >= 14;
            if ("Monthly".equalsIgnoreCase(frequency)) return diffInDays >= 30;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPopupMenu(View view, MyGardenPlantModel plant) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.plant_more_options_menu);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_name_plant) {
                if (listener != null) {
                    listener.onRenamePlantClick(plant);
                }
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
        ImageView ibMoreOptions;

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