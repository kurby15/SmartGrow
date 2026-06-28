package com.example.smartgrow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.SharedPreferences;

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

        // Isalpak ang data sa mga TextViews mo
        holder.tvName.setText(plant.getName());
        holder.tvSpecies.setText(plant.getSpecies());
        holder.tvDate.setText("Planted: " + plant.getDatePlanted());
        holder.tvStatus.setText(plant.getHealthStatus());

        // Simple dynamic background color para sa Badge base sa status (Mula sa pinakahuling log status ng halaman)
        if (plant.getHealthStatus().equalsIgnoreCase("Healthy")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#155724"));
        } else if (plant.getHealthStatus().equalsIgnoreCase("Diseased")) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#721C24"));
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#856404"));
        }

        // Fetch Recent Activity for Diary Card
        fetchDiaryRecentActivity(plant.getId(), holder);

        // 🌟 1. CLICK LISTENER PARA SA PENCIL/EDIT ICON
        holder.cardEditPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity(v.getContext());
                if (activity == null) return;

                // ✨ CLEANED UP: Inalis na natin si plant.getHealthStatus() sa Edit Sheet UI
                EditPlantBottomSheet editSheet = EditPlantBottomSheet.newInstance(
                        plant.getId(),
                        plant.getName(),
                        plant.getSpecies(),
                        plant.getDatePlanted(),
                        plant.getHealthStatus()
                );

                editSheet.show(activity.getSupportFragmentManager(), "EditPlantBottomSheetTag");
            }
        });

        // 🌟 2. CLICK LISTENER PARA SA ADD LOG BUTTON
        holder.btnAddLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity(v.getContext());
                if (activity == null) return;

                // Tawagin ang bagong gawang AddLogBottomSheet na may touch scroll behaviors
                AddLogBottomSheet addLogSheet = AddLogBottomSheet.newInstance(plant.getId());
                addLogSheet.show(activity.getSupportFragmentManager(), "AddLogBottomSheetTag");
            }
        });

        // 🌟 3. CLICK LISTENER PARA SA NOTIFICATION/REMINDER BELL ICON (BAGO!)
        holder.cardEditBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity(v.getContext());
                if (activity == null) return;

                // Bubuksan na si Bottom Sheet na may kasamang Watering, Fertilizer, at Sunlight parameters!
                PlantReminderBottomSheet reminderSheet = PlantReminderBottomSheet.newInstance(plant.getId());
                reminderSheet.show(activity.getSupportFragmentManager(), "PlantReminderBottomSheetTag");
            }
        });
    }

    private void fetchDiaryRecentActivity(String plantId, DiaryViewHolder holder) {
        if (plantId == null) return;

        SharedPreferences preferences = holder.itemView.getContext().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        String currentUsername = preferences.getString("current_username", "");

        if (currentUsername.isEmpty()) return;

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUsername).child("plants").child(plantId).child("logs");

        // Kunin ang pinakahuling log na ang 'watered' ay true
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        return plantList.size();
    }

    // Taga-bind ng mga IDs mula sa item_plant_card.xml
    public static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecies, tvDate, tvStatus, tvLastWatered;
        MaterialCardView cardEditPen;
        MaterialCardView cardEditBell; // 🌟 IDINAGDAG PARA SA BELL CARD CONTAINER
        MaterialButton btnAddLog;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_diary_plant_custom_name);
            tvSpecies = itemView.findViewById(R.id.tv_diary_plant_scientific_species);
            tvDate = itemView.findViewById(R.id.tv_diary_plant_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_diary_health_pill_text);
            tvLastWatered = itemView.findViewById(R.id.tv_diary_last_watered_activity_date);

            // 🔗 Ikonek ang mga operating panels mula sa card layout
            cardEditPen = itemView.findViewById(R.id.card_diary_item_edit_pen);
            cardEditBell = itemView.findViewById(R.id.card_diary_item_alert_bell); // 🔗 BININD ANG COMPONENT NG ALERTER BELL DITO!
            btnAddLog = itemView.findViewById(R.id.btn_diary_action_add_log);
        }
    }
}