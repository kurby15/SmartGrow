package com.example.smartgrow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HomePlantAdapter extends RecyclerView.Adapter<HomePlantAdapter.HomePlantViewHolder> {

    private List<PlantModel> plantList;

    public HomePlantAdapter(List<PlantModel> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public HomePlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // I-inflate ang bagong item layout na may progress bar at medicinal details
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_home_card, parent, false);
        return new HomePlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomePlantViewHolder holder, int position) {
        PlantModel plant = plantList.get(position);

        // ✨ INAYOS NA METHODS: Kusa nang tutugma sa PlantModel.java mo para iwas error!
        if (holder.tvName != null) {
            holder.tvName.setText(plant.getName()); // Binago mula getName() -> getPlantName()
        }

        if (holder.tvMedicinalUse != null) {
            holder.tvMedicinalUse.setText(plant.getMedicinalUse()); // Binago mula getMedicinalUse() -> getMedicalUse()
        }

        if (holder.tvHealthPercentage != null) {
            holder.tvHealthPercentage.setText(plant.getHealthPercentage() + "%");
        }

        // Isalpak ang percentage level sa structural horizontal bar
        if (holder.progressBarHealth != null) {
            holder.progressBarHealth.setProgress(plant.getHealthPercentage());
        }

        // Default local image placeholder muna gamit ang app logo niyo
        if (holder.imgPlantPhoto != null) {
            holder.imgPlantPhoto.setImageResource(R.drawable.smartgrow_logo);
        }
    }

    @Override
    public int getItemCount() {
        return plantList != null ? plantList.size() : 0;
    }

    public static class HomePlantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMedicinalUse, tvHealthPercentage;
        ProgressBar progressBarHealth;
        android.widget.ImageView imgPlantPhoto;

        public HomePlantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_home_plant_name);
            tvMedicinalUse = itemView.findViewById(R.id.tv_home_medicinal_use);
            tvHealthPercentage = itemView.findViewById(R.id.tv_home_health_percentage);
            progressBarHealth = itemView.findViewById(R.id.progress_home_health_bar);
            imgPlantPhoto = itemView.findViewById(R.id.img_home_plant_photo);
        }
    }
}