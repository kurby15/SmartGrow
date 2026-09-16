package com.example.smartgrow.plants;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartgrow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SetReminderFragment extends Fragment {

    private static final String TAG = "SetReminderFragment";
    private ImageView btnBack;
    private TextView tvDesc;
    private RecyclerView rvReminderPlants;
    private ReminderPlantAdapter reminderPlantAdapter;
    private List<MyGardenPlantModel> plantList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private MyGardenPlantModel selectedPlant = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_reminder, container, false);

        btnBack = view.findViewById(R.id.btn_back);
        tvDesc = view.findViewById(R.id.tv_desc);
        rvReminderPlants = view.findViewById(R.id.rv_reminder_plants);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        plantList = new ArrayList<>();
        rvReminderPlants.setLayoutManager(new LinearLayoutManager(getContext()));

        reminderPlantAdapter = new ReminderPlantAdapter(getContext(), plantList, new ReminderPlantAdapter.OnPlantSelectedListener() {
            @Override
            public void onPlantSelected(MyGardenPlantModel plant) {
                selectedPlant = plant;
            }

            @Override
            public void onRemoveReminder(MyGardenPlantModel plant) {
                if (plant.getId() != null) {

                    db.collection("diary").document(plant.getId())
                            .update("reminders", null)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Reminder removed for " + plant.getPlantName(), Toast.LENGTH_SHORT).show();
                                fetchUserPlantsFromDiary();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to remove reminder", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        rvReminderPlants.setAdapter(reminderPlantAdapter);

        Button btnSetReminder = view.findViewById(R.id.btn_set_reminder);
        if (btnSetReminder != null) {
            btnSetReminder.setOnClickListener(v -> {
                if (selectedPlant != null) {
                    PlantReminderBottomSheet bottomSheet = PlantReminderBottomSheet.newInstance(selectedPlant.getId());
                    bottomSheet.show(getParentFragmentManager(), "PlantReminderBottomSheet");
                } else {
                    Toast.makeText(getContext(), "Please select a plant first", Toast.LENGTH_SHORT).show();
                }
            });
        }
        fetchUserPlantsFromDiary();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        return view;
    }

    private void fetchUserPlantsFromDiary() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        db.collection("diary")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    plantList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        MyGardenPlantModel plant = document.toObject(MyGardenPlantModel.class);
                        if (plant.getId() == null) plant.setId(document.getId());
                        plantList.add(plant);
                    }

                    if (plantList.isEmpty()) {
                        tvDesc.setVisibility(View.VISIBLE);
                        rvReminderPlants.setVisibility(View.GONE);
                    } else {
                        tvDesc.setVisibility(View.GONE);
                        rvReminderPlants.setVisibility(View.VISIBLE);
                    }

                    reminderPlantAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching plants from diary", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to fetch plants", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}