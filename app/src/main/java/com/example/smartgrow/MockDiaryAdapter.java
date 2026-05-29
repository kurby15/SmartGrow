package com.example.smartgrow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class MockDiaryAdapter extends RecyclerView.Adapter<MockDiaryAdapter.DiaryViewHolder> {

    private List<PlantModel> plantList;

    public MockDiaryAdapter(List<PlantModel> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // I-inflate ang ginawa mong item_plant_card xml layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_card, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        PlantModel plant = plantList.get(position);

        // Isalpak ang pekeng data sa mga TextViews mo
        holder.tvName.setText(plant.getName());
        holder.tvSpecies.setText(plant.getSpecies());
        holder.tvDate.setText("Planted: " + plant.getDatePlanted());
        holder.tvStatus.setText(plant.getHealthStatus());

        // Simple dynamic background color para sa Badge base sa status
        if (plant.getHealthStatus().equalsIgnoreCase("Healthy")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#155724"));
        } else if (plant.getHealthStatus().equalsIgnoreCase("Diseased")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#721C24"));
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#856404"));
        }

        // 🌟 CLICK LISTENER PARA SA PENCIL/EDIT ICON
        holder.cardEditPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hatiin ang Context para makuha ang FragmentActivity na kailangan ng BottomSheet
                FragmentActivity activity = (FragmentActivity) v.getContext();

                // Tawagin ang newInstance ng Edit Sheet para automatic may pre-filled text ang mga kahon
                EditPlantBottomSheet editSheet = EditPlantBottomSheet.newInstance(
                        plant.getName(),
                        plant.getSpecies(),
                        plant.getDatePlanted(),
                        plant.getHealthStatus()
                );

                // Ipakita na ang magandang Edit Sheet sa screen
                editSheet.show(activity.getSupportFragmentManager(), "EditPlantBottomSheetTag");
            }
        });
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    // Taga-bind ng mga IDs mula sa item_plant_card.xml
    public static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecies, tvDate, tvStatus;
        MaterialCardView cardEditPen; // Sinama natin si pencil frame control dito

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_diary_plant_custom_name);
            tvSpecies = itemView.findViewById(R.id.tv_diary_plant_scientific_species);
            tvDate = itemView.findViewById(R.id.tv_diary_plant_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_diary_health_pill_text);

            // 🔗 Ikonek ang Pencil click zone container ID mula sa card layout
            cardEditPen = itemView.findViewById(R.id.card_diary_item_edit_pen);
        }
    }
}