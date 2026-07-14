package com.example.smartgrow;

import android.content.Context;
import android.content.SharedPreferences;
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

    public DiaryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        if (getActivity() != null) {
            SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
            currentUsername = preferences.getString("current_username", "");
        }

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

        if (!currentUsername.isEmpty()) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
            fetchPlantsFromFirebase();
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