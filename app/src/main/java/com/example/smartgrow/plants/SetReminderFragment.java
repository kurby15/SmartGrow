package com.example.smartgrow.plants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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

    private ImageView btnBack;
    // Inside your Fragment class fields:

    private TextView tvDesc;
    private RecyclerView rvReminderPlants;
    private ReminderPlantAdapter reminderPlantAdapter;
    private List<PlantModel> plantList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_reminder, container, false); // O ang tamang layout name mo

        // Initialize Views
        btnBack = view.findViewById(R.id.btn_back);
        tvDesc = view.findViewById(R.id.tv_desc);
        rvReminderPlants = view.findViewById(R.id.rv_reminder_plants);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        plantList = new ArrayList<>();
        rvReminderPlants.setLayoutManager(new LinearLayoutManager(getContext()));

        reminderPlantAdapter = new ReminderPlantAdapter(getContext(), plantList, plant -> {
            Toast.makeText(getContext(), "Selected: " + plant.getPlantName(), Toast.LENGTH_SHORT).show();
        });

        rvReminderPlants.setAdapter(reminderPlantAdapter);
        fetchUserPlants();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        return view;
    }

    private void fetchUserPlants() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        db.collection("users").document(userId).collection("plants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    plantList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        PlantModel plant = document.toObject(PlantModel.class);
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
                    Toast.makeText(getContext(), "Failed to fetch plants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}