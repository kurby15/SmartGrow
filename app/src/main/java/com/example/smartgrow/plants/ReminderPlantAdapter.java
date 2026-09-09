package com.example.smartgrow.plants;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.smartgrow.R;
import java.util.List;

public class ReminderPlantAdapter extends RecyclerView.Adapter<ReminderPlantAdapter.ViewHolder> {

    private Context context;
    private List<PlantModel> plantList;
    private OnPlantSelectedListener listener;

    public interface OnPlantSelectedListener {
        void onPlantSelected(PlantModel plant);
    }

    public ReminderPlantAdapter(Context context, List<PlantModel> plantList, OnPlantSelectedListener listener) {
        this.context = context;
        this.plantList = plantList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder_plant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantModel plant = plantList.get(position);
        holder.tvPlantName.setText(plant.getPlantName());
        holder.tvScientificName.setText(plant.getScientificName());

        if (plant.getPlantBitmap() != null) {
            holder.ivPlantThumb.setImageBitmap(plant.getPlantBitmap());
        }

        holder.btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlantSelected(plant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlantThumb;
        TextView tvPlantName, tvScientificName;
        MaterialButton btnSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlantThumb = itemView.findViewById(R.id.iv_plant_thumb);
            tvPlantName = itemView.findViewById(R.id.tv_plant_name);
            tvScientificName = itemView.findViewById(R.id.tv_scientific_name);
            btnSelect = itemView.findViewById(R.id.btn_select_plant);
        }
    }
}