package com.example.smartgrow.plants;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.util.Base64;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class MockDiaryAdapter extends RecyclerView.Adapter<MockDiaryAdapter.DiaryViewHolder> {

    private List<PlantModel> plantList;

    public MockDiaryAdapter(List<PlantModel> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_card, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        PlantModel plant = plantList.get(position);

        holder.tvName.setText(plant.getName());
        holder.tvSpecies.setText(plant.getSpecies());
        holder.tvDate.setText("Planted: " + plant.getDatePlanted());
        holder.tvStatus.setText(plant.getHealthStatus());

        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty()) {
            if (plant.getImageUrl().startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(plant.getImageUrl())
                        .placeholder(R.drawable.smartgrow_logo)
                        .into(holder.imgPlant);
            } else {
                try {
                    byte[] decodedString = Base64.decode(plant.getImageUrl(), Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.imgPlant.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.imgPlant.setImageResource(R.drawable.smartgrow_logo);
                }
            }
        } else {
            holder.imgPlant.setImageResource(R.drawable.smartgrow_logo);
        }

        if (plant.getHealthStatus().equalsIgnoreCase("Healthy")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#155724"));
        } else if (plant.getHealthStatus().equalsIgnoreCase("Diseased")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#721C24"));
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#856404"));
        }

        fetchDiaryRecentActivity(plant.getId(), holder);

        holder.cardEditPen.setOnClickListener(v -> {
            FragmentActivity activity = getActivity(v.getContext());
            if (activity == null) return;

            EditPlantBottomSheet editSheet = EditPlantBottomSheet.newInstance(
                    plant.getId(),
                    plant.getName(),
                    plant.getSpecies(),
                    plant.getMedicinalUse(),
                    plant.getDatePlanted(),
                    plant.getHealthStatus(),
                    plant.getImageUrl()
            );

            editSheet.show(activity.getSupportFragmentManager(), "EditPlantBottomSheetTag");
        });

        holder.btnAddLog.setOnClickListener(v -> {
            FragmentActivity activity = getActivity(v.getContext());
            if (activity == null) return;
            AddLogBottomSheet addLogSheet = AddLogBottomSheet.newInstance(plant.getId());
            addLogSheet.show(activity.getSupportFragmentManager(), "AddLogBottomSheetTag");
        });

        holder.cardEditBell.setOnClickListener(v -> {
            FragmentActivity activity = getActivity(v.getContext());
            if (activity == null) return;
            PlantReminderBottomSheet reminderSheet = PlantReminderBottomSheet.newInstance(plant.getId());
            reminderSheet.show(activity.getSupportFragmentManager(), "PlantReminderBottomSheetTag");
        });
    }

    private void fetchDiaryRecentActivity(String plantId, DiaryViewHolder holder) {
        if (plantId == null) return;

        SharedPrefManager prefManager = SharedPrefManager.getInstance(holder.itemView.getContext());
        String currentUsername = prefManager.getUsername();

        if (currentUsername == null || currentUsername.isEmpty() || currentUsername.equals("unknown")) return;

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUsername).child("plants").child(plantId).child("logs");

        logsRef.orderByChild("watered").equalTo(true).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                        LogModel lastLog = logSnapshot.getValue(LogModel.class);
                        if (lastLog != null && holder.tvLastWatered != null) {
                            holder.tvLastWatered.setText(lastLog.getDate());
                        }
                    }
                } else {
                    if (holder.tvLastWatered != null) {
                        holder.tvLastWatered.setText("No activity yet");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private FragmentActivity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) {
                return (FragmentActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return plantList != null ? plantList.size() : 0;
    }

    public static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecies, tvDate, tvStatus, tvLastWatered;
        ImageView imgPlant;
        MaterialCardView cardEditPen;
        MaterialCardView cardEditBell;
        MaterialButton btnAddLog;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_diary_plant_custom_name);
            tvSpecies = itemView.findViewById(R.id.tv_diary_plant_scientific_species);
            tvDate = itemView.findViewById(R.id.tv_diary_plant_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_diary_health_pill_text);
            tvLastWatered = itemView.findViewById(R.id.tv_diary_last_watered_activity_date);
            imgPlant = itemView.findViewById(R.id.img_diary_plant_visual);
            cardEditPen = itemView.findViewById(R.id.card_diary_item_edit_pen);
            cardEditBell = itemView.findViewById(R.id.card_diary_item_alert_bell);
            btnAddLog = itemView.findViewById(R.id.btn_diary_action_add_log);
        }
    }
}
