package com.example.smartgrow.plants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.util.Base64;
import com.bumptech.glide.Glide;

public class HomePlantAdapter extends RecyclerView.Adapter<HomePlantAdapter.HomePlantViewHolder> {

    private List<PlantModel> plantList;

    public HomePlantAdapter(List<PlantModel> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public HomePlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_home_card, parent, false);
        return new HomePlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomePlantViewHolder holder, int position) {
        PlantModel plant = plantList.get(position);

        if (holder.tvName != null) holder.tvName.setText(plant.getName());
        if (holder.tvMedicinalUse != null) holder.tvMedicinalUse.setText(plant.getMedicinalUse());
        if (holder.tvHealthPercentage != null) holder.tvHealthPercentage.setText(plant.getHealthPercentage() + "%");
        if (holder.progressBarHealth != null) holder.progressBarHealth.setProgress(plant.getHealthPercentage());

        fetchRecentActivity(plant.getId(), holder);

        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty()) {
            if (plant.getImageUrl().startsWith("http")) {
                Glide.with(holder.itemView.getContext()).load(plant.getImageUrl()).placeholder(R.drawable.smartgrow_logo).into(holder.imgPlantPhoto);
            } else {
                try {
                    byte[] decodedString = android.util.Base64.decode(plant.getImageUrl(), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.imgPlantPhoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.imgPlantPhoto.setImageResource(R.drawable.smartgrow_logo);
                }
            }
        } else {
            holder.imgPlantPhoto.setImageResource(R.drawable.smartgrow_logo);
        }
    }

    private void fetchRecentActivity(String plantId, HomePlantViewHolder holder) {
        if (plantId == null) return;

        SharedPrefManager prefManager = SharedPrefManager.getInstance(holder.itemView.getContext());
        String currentUsername = prefManager.getUsername();

        if (currentUsername == null || currentUsername.isEmpty() || currentUsername.equals("unknown")) return;

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUsername).child("plants").child(plantId).child("logs");

        logsRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.layoutWater.setVisibility(View.GONE);
                holder.layoutSun.setVisibility(View.GONE);
                holder.layoutFertilizer.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                        LogModel lastLog = logSnapshot.getValue(LogModel.class);
                        if (lastLog != null) {
                            if (lastLog.isWatered()) holder.layoutWater.setVisibility(View.VISIBLE);
                            if (lastLog.getSunlightExposure() != null && !lastLog.getSunlightExposure().isEmpty() && !lastLog.getSunlightExposure().equalsIgnoreCase("None")) {
                                holder.layoutSun.setVisibility(View.VISIBLE);
                            }
                            if (lastLog.isFertilized()) holder.layoutFertilizer.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override public int getItemCount() { return plantList != null ? plantList.size() : 0; }

    public static class HomePlantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMedicinalUse, tvHealthPercentage;
        ProgressBar progressBarHealth;
        android.widget.ImageView imgPlantPhoto;
        View layoutWater, layoutSun, layoutFertilizer;

        public HomePlantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_home_plant_name);
            tvMedicinalUse = itemView.findViewById(R.id.tv_home_medicinal_use);
            tvHealthPercentage = itemView.findViewById(R.id.tv_home_health_percentage);
            progressBarHealth = itemView.findViewById(R.id.progress_home_health_bar);
            imgPlantPhoto = itemView.findViewById(R.id.img_home_plant_photo);
            layoutWater = itemView.findViewById(R.id.layout_badge_water);
            layoutSun = itemView.findViewById(R.id.layout_badge_sun);
            layoutFertilizer = itemView.findViewById(R.id.layout_badge_fertilizer);
        }
    }
}
