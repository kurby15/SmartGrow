package com.example.smartgrow;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    // 🟢 NILINIS NA: Tinanggal na ang header cards (cardDiaryNotification, cardDiaryGlobal, cardDiaryProfile)
    private MaterialCardView cardBtnAddNewDiary;
    private EditText etSearchPlants;
    private RecyclerView rvPlantDiaryList;

    private List<PlantModel> plantList = new ArrayList<>();
    private List<PlantModel> filteredList = new ArrayList<>();
    private MockDiaryAdapter adapter;
    private DatabaseReference databaseReference;
    private String currentUsername;

    public DiaryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        // 🔗 Bind Search Context at Control Buttons
        etSearchPlants = view.findViewById(R.id.et_search_plants);
        cardBtnAddNewDiary = view.findViewById(R.id.card_btn_add_new_diary);

        // 🔗 Bind ang Target List View Container (RecyclerView)
        rvPlantDiaryList = view.findViewById(R.id.rv_plant_diary_list);
        if (rvPlantDiaryList != null) {
            rvPlantDiaryList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPlantDiaryList.setHasFixedSize(true);
        }

        // Initialize adapter
        adapter = new MockDiaryAdapter(filteredList);
        rvPlantDiaryList.setAdapter(adapter);

        if (!currentUsername.isEmpty()) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            fetchPlantsFromFirebase();
        }

        // 🛠️ Setup Click Actions
        setupClickListeners();

        // Setup Search logic
        setupSearchLogic();

        return view;
    }

    private void fetchPlantsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                plantList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PlantModel plant = dataSnapshot.getValue(PlantModel.class);
                    if (plant != null) {
                        plantList.add(plant);
                    }
                }
                filterList(etSearchPlants.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSearchLogic() {
        etSearchPlants.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(plantList);
        } else {
            for (PlantModel plant : plantList) {
                if (plant.getName().toLowerCase().contains(query.toLowerCase()) ||
                    plant.getSpecies().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(plant);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        // 🟢 NILINIS NA LISTENERS: Ang natira na lang ay ang Add New Diary Button
        if (cardBtnAddNewDiary != null) {
            cardBtnAddNewDiary.setOnClickListener(v -> {
                AddPlantBottomSheetActivity addPlantSheet = new AddPlantBottomSheetActivity();
                addPlantSheet.show(getParentFragmentManager(), "AddPlantBottomSheetTag");
            });
        }
    }
}