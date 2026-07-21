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

import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    private MaterialCardView cardBtnAddNewDiary;
    private EditText etSearchPlants;
    private RecyclerView rvPlantDiaryList;

    private List<PlantModel> plantList = new ArrayList<>();
    private List<PlantModel> filteredList = new ArrayList<>();
    private MockDiaryAdapter adapter;
    private DatabaseReference databaseReference;
    private String currentUsername = "";
    private SharedPrefManager prefManager;

    public DiaryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        // 🔐 SECURE DATA FETCH: Gamitin ang SharedPrefManager
        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();

        etSearchPlants = view.findViewById(R.id.et_search_plants);
        cardBtnAddNewDiary = view.findViewById(R.id.card_btn_add_new_diary);
        rvPlantDiaryList = view.findViewById(R.id.rv_plant_diary_list);

        if (rvPlantDiaryList != null) {
            rvPlantDiaryList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPlantDiaryList.setHasFixedSize(true);
        }

        adapter = new MockDiaryAdapter(filteredList);
        if (rvPlantDiaryList != null) {
            rvPlantDiaryList.setAdapter(adapter);
        }

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            fetchPlantsFromFirebase();
        } else {
            Toast.makeText(getContext(), "User session not found.", Toast.LENGTH_SHORT).show();
        }

        setupClickListeners();
        setupSearchLogic();

        return view;
    }

    private void fetchPlantsFromFirebase() {
        if (databaseReference == null) return;

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                plantList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PlantModel plant = dataSnapshot.getValue(PlantModel.class);
                    if (plant != null) {
                        plantList.add(plant);
                    }
                }

                if (etSearchPlants != null && etSearchPlants.getText() != null) {
                    filterList(etSearchPlants.getText().toString());
                } else {
                    filterList("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSearchLogic() {
        if (etSearchPlants == null) return;

        etSearchPlants.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAdded()) {
                    filterList(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(plantList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (PlantModel plant : plantList) {
                if ((plant.getName() != null && plant.getName().toLowerCase().contains(lowerCaseQuery)) ||
                    (plant.getSpecies() != null && plant.getSpecies().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(plant);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupClickListeners() {
        if (cardBtnAddNewDiary != null) {
            cardBtnAddNewDiary.setOnClickListener(v -> {
                if (isAdded()) {
                    AddPlantBottomSheetActivity addPlantSheet = new AddPlantBottomSheetActivity();
                    addPlantSheet.show(getParentFragmentManager(), "AddPlantBottomSheetTag");
                }
            });
        }
    }
}
