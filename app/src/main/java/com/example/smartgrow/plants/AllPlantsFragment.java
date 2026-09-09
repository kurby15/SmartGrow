package com.example.smartgrow.plants;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllPlantsFragment extends Fragment {

    private RecyclerView rvAllPlantList;
    private EditText etSearchAllPlants;
    private LinearLayout layoutViewLess;

    private MyGardenPlantAdapter adapter;
    private List<MyGardenPlantModel> plantList = new ArrayList<>();
    private List<MyGardenPlantModel> fullPlantList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration plantsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_plants, container, false); // Palitan ng layout xml mo kung iba ang pangalan

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvAllPlantList = view.findViewById(R.id.rv_all_plant_list);
        etSearchAllPlants = view.findViewById(R.id.et_search_all_plants);
        layoutViewLess = view.findViewById(R.id.layout_view_less);

        if (layoutViewLess != null) {
            layoutViewLess.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        setupRecyclerView();
        setupSearch();
        listenToAllPlants();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (plantsListener != null) {
            plantsListener.remove();
        }
    }

    private void setupRecyclerView() {
        adapter = new MyGardenPlantAdapter(plantList, new MyGardenPlantAdapter.OnPlantClickListener() {
            @Override
            public void onAddReminderClick(MyGardenPlantModel plant) {
                PlantReminderBottomSheet bottomSheet = PlantReminderBottomSheet.newInstance(plant.getId());
                bottomSheet.show(getParentFragmentManager(), "PlantReminderBottomSheet");
            }

            @Override
            public void onPlantClick(MyGardenPlantModel plant) {
                if (plant != null && plant.getId() != null) {
                    Intent intent = new Intent(getContext(), PlantDetailsActivity.class);
                    intent.putExtra("plant_id", plant.getId());
                    startActivity(intent);
                }
            }

            @Override
            public void onRenamePlantClick(MyGardenPlantModel plant) {
                List<String> suggestions = extractSuggestionsFromPlant(plant);
                showNamePlantBottomSheet(plant.getPlantName(), suggestions, newName -> {
                    plant.setPlantName(newName);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    updatePlantNameInFirestore(plant.getId(), newName);
                });
            }

            @Override
            public void onRemovePlantClick(MyGardenPlantModel plant) {
                if (plant != null && plant.getId() != null) {
                    plantList.remove(plant);
                    fullPlantList.remove(plant);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    deletePlantFromFirestore(plant.getId(), plant.getPlantName());
                }
            }
        });

        if (getContext() != null) {
            rvAllPlantList.setLayoutManager(new GridLayoutManager(getContext(), 2));
            rvAllPlantList.setAdapter(adapter);
        }
    }

    private void setupSearch() {
        if (etSearchAllPlants != null) {
            etSearchAllPlants.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filter(String query) {
        String lowerCaseQuery = (query != null) ? query.toLowerCase().trim() : "";
        plantList.clear();

        if (lowerCaseQuery.isEmpty()) {
            plantList.addAll(fullPlantList);
        } else {
            for (MyGardenPlantModel plant : fullPlantList) {
                boolean matchesName = plant.getPlantName() != null && plant.getPlantName().toLowerCase().contains(lowerCaseQuery);
                boolean matchesScientific = plant.getScientificName() != null && plant.getScientificName().toLowerCase().contains(lowerCaseQuery);
                if (matchesName || matchesScientific) {
                    plantList.add(plant);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void listenToAllPlants() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        plantsListener = db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error listening to plant changes", error);
                        return;
                    }

                    if (value != null) {
                        fullPlantList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            MyGardenPlantModel plant = doc.toObject(MyGardenPlantModel.class);
                            if (plant != null) {
                                if (plant.getId() == null || plant.getId().isEmpty()) {
                                    plant.setId(doc.getId());
                                }
                                fullPlantList.add(plant);
                            }
                        }

                        if (etSearchAllPlants != null && etSearchAllPlants.getText() != null && !etSearchAllPlants.getText().toString().isEmpty()) {
                            filter(etSearchAllPlants.getText().toString());
                        } else {
                            plantList.clear();
                            plantList.addAll(fullPlantList);
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private void showNamePlantBottomSheet(String currentName, List<String> suggestions, PlantNameBottomSheetFragment.OnPlantNameUpdatedListener updateListener) {
        PlantNameBottomSheetFragment bottomSheet = PlantNameBottomSheetFragment.newInstance(currentName, suggestions);
        bottomSheet.setOnPlantNameUpdatedListener(updateListener);
        bottomSheet.show(getParentFragmentManager(), "PlantNameBottomSheet");
    }

    private List<String> extractSuggestionsFromPlant(MyGardenPlantModel plant) {
        if (plant != null && plant.getRawAnalysisJson() != null && !plant.getRawAnalysisJson().isEmpty()) {
            return PlantAnalyzer.extractPlantSuggestionsFromRawJson(plant.getRawAnalysisJson());
        }
        return new ArrayList<>();
    }

    private void updatePlantNameInFirestore(String documentId, String newName) {
        if (documentId == null || documentId.isEmpty()) return;
        db.collection("diary").document(documentId).update("plantName", newName);
    }

    private void deletePlantFromFirestore(String documentId, String plantName) {
        if (documentId == null || documentId.isEmpty()) return;
        db.collection("diary").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Removed " + (plantName != null ? plantName : "item"), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}