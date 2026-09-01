package com.example.smartgrow.plants;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.example.smartgrow.history.SnapHistoryAdapter;
import com.example.smartgrow.history.SnapHistoryModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyGardenFragment extends Fragment {

    // Header & Navigation Tab Elements
    private LinearLayout tabMyGarden, tabSnapHistory;
    private TextView tvTabMyGarden, tvTabSnapHistory;
    private View indicatorMyGarden, indicatorSnapHistory;
    private ImageButton ibTopMore;
    private EditText etSearchPlants;
    private LinearLayout layoutViewAll;

    // Content Containers & RecyclerViews
    private View layoutMyGardenContent, layoutSnapHistoryContent;
    private RecyclerView rvPlantList, rvSnapHistoryList;

    // Adapters & Active Display Lists
    private MyGardenPlantAdapter gardenAdapter;
    private SnapHistoryAdapter snapAdapter;
    private List<MyGardenPlantModel> plantList = new ArrayList<>();
    private List<SnapHistoryModel> snapList = new ArrayList<>();

    // Master Cache Lists for Search Filtering
    private List<MyGardenPlantModel> fullPlantList = new ArrayList<>();
    private List<SnapHistoryModel> fullSnapList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Real-time Firestore Listener Handles
    private ListenerRegistration diaryListener;
    private ListenerRegistration snapHistoryListener;

    // Active/Inactive Tab Colors
    private final int COLOR_ACTIVE = Color.parseColor("#FFFFFF");
    private final int COLOR_INACTIVE = Color.parseColor("#2E4336");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_garden_main, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerViews();

        if (ibTopMore != null) {
            ibTopMore.setOnClickListener(this::showTopPopupMenu);
        }

        // Setup real-time live search filter
        setupSearchFilter();

        // Attach Real-time Listeners
        listenToDiaryData();
        listenToSnapHistoryData();

        // Tab Switch Listeners
        View.OnClickListener myGardenClickListener = v -> switchToMyGarden();
        View.OnClickListener snapHistoryClickListener = v -> switchToSnapHistory();

        if (tabMyGarden != null) tabMyGarden.setOnClickListener(myGardenClickListener);
        if (tvTabMyGarden != null) tvTabMyGarden.setOnClickListener(myGardenClickListener);

        if (tabSnapHistory != null) tabSnapHistory.setOnClickListener(snapHistoryClickListener);
        if (tvTabSnapHistory != null) tvTabSnapHistory.setOnClickListener(snapHistoryClickListener);

        switchToMyGarden();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detach real-time listeners to avoid memory leaks
        if (diaryListener != null) {
            diaryListener.remove();
        }
        if (snapHistoryListener != null) {
            snapHistoryListener.remove();
        }
    }

    private void initViews(View view) {
        tabMyGarden = view.findViewById(R.id.tab_my_garden);
        tabSnapHistory = view.findViewById(R.id.tab_snap_history);

        tvTabMyGarden = view.findViewById(R.id.tv_tab_my_garden);
        tvTabSnapHistory = view.findViewById(R.id.tv_tab_snap_history);

        indicatorMyGarden = view.findViewById(R.id.indicator_my_garden);
        indicatorSnapHistory = view.findViewById(R.id.indicator_snap_history);

        layoutMyGardenContent = view.findViewById(R.id.layout_my_garden_content);
        layoutSnapHistoryContent = view.findViewById(R.id.layout_snap_history_content);

        rvPlantList = view.findViewById(R.id.rv_plant_list);
        rvSnapHistoryList = view.findViewById(R.id.rv_snap_history_list);

        ibTopMore = view.findViewById(R.id.ib_filter_more);

        // Bind single shared search EditText from XML
        etSearchPlants = view.findViewById(R.id.et_search_plants);

        // Bind View All Button and navigate to AllPlantsActivity
        layoutViewAll = view.findViewById(R.id.layout_view_all);
        if (layoutViewAll != null) {
            layoutViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AllPlantsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupSearchFilter() {
        if (etSearchPlants != null) {
            etSearchPlants.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterLists(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterLists(String query) {
        String lowerCaseQuery = (query != null) ? query.toLowerCase().trim() : "";

        // Filter My Garden Tab
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
        if (gardenAdapter != null) {
            gardenAdapter.notifyDataSetChanged();
        }

        // Filter Scan History Tab
        snapList.clear();
        if (lowerCaseQuery.isEmpty()) {
            snapList.addAll(fullSnapList);
        } else {
            for (SnapHistoryModel snap : fullSnapList) {
                boolean matchesName = snap.getPlantName() != null && snap.getPlantName().toLowerCase().contains(lowerCaseQuery);
                boolean matchesScientific = snap.getScientificName() != null && snap.getScientificName().toLowerCase().contains(lowerCaseQuery);
                if (matchesName || matchesScientific) {
                    snapList.add(snap);
                }
            }
        }
        if (snapAdapter != null) {
            snapAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerViews() {
        // --- 1. My Garden Adapter ---
        gardenAdapter = new MyGardenPlantAdapter(plantList, new MyGardenPlantAdapter.OnPlantClickListener() {
            @Override
            public void onAddReminderClick(MyGardenPlantModel plant) {
                Toast.makeText(getContext(), "Add reminder for " + plant.getPlantName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRenamePlantClick(MyGardenPlantModel plant) {
                List<String> suggestions = extractSuggestionsFromPlant(plant);

                showNamePlantBottomSheet(plant.getPlantName(), suggestions, newName -> {
                    plant.setPlantName(newName);
                    if (gardenAdapter != null) {
                        gardenAdapter.notifyDataSetChanged();
                    }
                    updatePlantNameInFirestore("diary", plant.getId(), newName);
                });
            }

            @Override
            public void onRemovePlantClick(MyGardenPlantModel plant) {
                if (plant != null && plant.getId() != null) {
                    // Optimistic local update
                    plantList.remove(plant);
                    fullPlantList.remove(plant);
                    if (gardenAdapter != null) {
                        gardenAdapter.notifyDataSetChanged();
                    }
                    deletePlantFromFirestore("diary", plant.getId(), plant.getPlantName(), null);
                }
            }
        });

        if (rvPlantList != null) {
            rvPlantList.setLayoutManager(new GridLayoutManager(getContext(), 2));
            rvPlantList.setAdapter(gardenAdapter);
        }

        // --- 2. Snap History Adapter ---
        snapAdapter = new SnapHistoryAdapter(snapList, new SnapHistoryAdapter.OnSnapClickListener() {
            @Override
            public void onSnapClick(SnapHistoryModel snap) {
                List<String> suggestions = extractSuggestionsFromSnap(snap);

                showNamePlantBottomSheet(snap.getPlantName(), suggestions, newName -> {
                    snap.setPlantName(newName);
                    if (snapAdapter != null) {
                        snapAdapter.notifyDataSetChanged();
                    }
                    updatePlantNameInFirestore("diary_history", snap.getId(), newName);
                });
            }

            @Override
            public void onAddToGardenClick(SnapHistoryModel snap) {
                if (getContext() != null) {
                    saveSnapToGarden(snap);
                }
            }

            @Override
            public void onEditNameClick(SnapHistoryModel snap) {
                List<String> suggestions = extractSuggestionsFromSnap(snap);

                showNamePlantBottomSheet(snap.getPlantName(), suggestions, newName -> {
                    snap.setPlantName(newName);
                    if (snapAdapter != null) {
                        snapAdapter.notifyDataSetChanged();
                    }
                    updatePlantNameInFirestore("diary_history", snap.getId(), newName);
                });
            }

            @Override
            public void onDeleteSnapClick(SnapHistoryModel snap) {
                if (snap != null && snap.getId() != null) {
                    // Optimistic local update
                    snapList.remove(snap);
                    fullSnapList.remove(snap);
                    if (snapAdapter != null) {
                        snapAdapter.notifyDataSetChanged();
                    }
                    deletePlantFromFirestore("diary_history", snap.getId(), snap.getPlantName(), null);
                }
            }
        });

        if (rvSnapHistoryList != null) {
            rvSnapHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvSnapHistoryList.setAdapter(snapAdapter);
        }
    }

    private void showNamePlantBottomSheet(String currentName, List<String> suggestions, PlantNameBottomSheetFragment.OnPlantNameUpdatedListener updateListener) {
        if (!isAdded() || getContext() == null) return;

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

    private List<String> extractSuggestionsFromSnap(SnapHistoryModel snap) {
        if (snap != null && snap.getRawAnalysisJson() != null && !snap.getRawAnalysisJson().isEmpty()) {
            return PlantAnalyzer.extractPlantSuggestionsFromRawJson(snap.getRawAnalysisJson());
        }
        return new ArrayList<>();
    }

    private void updatePlantNameInFirestore(String collectionName, String documentId, String newName) {
        if (documentId == null || documentId.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Plant renamed to: " + newName, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        db.collection(collectionName).document(documentId)
                .update("plantName", newName)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Plant renamed to: " + newName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to update plant name", e));
    }

    private void deletePlantFromFirestore(String collectionName, String documentId, String plantName, Runnable onSuccessCallback) {
        if (documentId == null || documentId.isEmpty()) return;

        db.collection(collectionName).document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Removed " + (plantName != null ? plantName : "item") + " successfully", Toast.LENGTH_SHORT).show();
                    }
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to delete item from " + collectionName, e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Real-time Firestore Snapshot Listener for "diary" Collection
    private void listenToDiaryData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            plantList.clear();
            fullPlantList.clear();
            if (gardenAdapter != null) gardenAdapter.notifyDataSetChanged();
            if (snapAdapter != null) snapAdapter.setGardenPlantNames(new ArrayList<>());
            return;
        }

        diaryListener = db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error listening to diary data", error);
                        return;
                    }

                    if (value != null) {
                        fullPlantList.clear();
                        List<String> activeGardenPlantNames = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : value) {
                            MyGardenPlantModel plant = doc.toObject(MyGardenPlantModel.class);
                            if (plant != null) {
                                if (plant.getId() == null || plant.getId().isEmpty()) {
                                    plant.setId(doc.getId());
                                }
                                fullPlantList.add(plant);

                                if (plant.getPlantName() != null) {
                                    activeGardenPlantNames.add(plant.getPlantName().trim());
                                }
                            }
                        }

                        if (etSearchPlants != null && etSearchPlants.getText() != null && !etSearchPlants.getText().toString().isEmpty()) {
                            filterLists(etSearchPlants.getText().toString());
                        } else {
                            plantList.clear();
                            plantList.addAll(fullPlantList);
                            if (gardenAdapter != null) {
                                gardenAdapter.notifyDataSetChanged();
                            }
                        }

                        // Pass list of plants currently in garden to the Snap History Adapter
                        if (snapAdapter != null) {
                            snapAdapter.setGardenPlantNames(activeGardenPlantNames);
                        }
                    }
                });
    }

    // Real-time Firestore Snapshot Listener for "diary_history" Collection
    private void listenToSnapHistoryData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            snapList.clear();
            fullSnapList.clear();
            if (snapAdapter != null) snapAdapter.notifyDataSetChanged();
            return;
        }

        snapHistoryListener = db.collection("diary_history")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error listening to snap history", error);
                        return;
                    }

                    if (value != null) {
                        fullSnapList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            SnapHistoryModel snap = doc.toObject(SnapHistoryModel.class);
                            if (snap != null) {
                                if (snap.getId() == null || snap.getId().isEmpty()) {
                                    snap.setId(doc.getId());
                                }
                                fullSnapList.add(snap);
                            }
                        }

                        if (etSearchPlants != null && etSearchPlants.getText() != null && !etSearchPlants.getText().toString().isEmpty()) {
                            filterLists(etSearchPlants.getText().toString());
                        } else {
                            snapList.clear();
                            snapList.addAll(fullSnapList);
                            if (snapAdapter != null) {
                                snapAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private void saveSnapToGarden(SnapHistoryModel snap) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (snap == null || currentUser == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login to save.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String userId = currentUser.getUid();
        String docId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> diaryEntry = new HashMap<>();
        diaryEntry.put("id", docId);
        diaryEntry.put("userId", userId);
        diaryEntry.put("plantName", snap.getPlantName() != null ? snap.getPlantName() : "Unknown Plant");
        diaryEntry.put("scientificName", snap.getScientificName() != null ? snap.getScientificName() : "N/A");
        diaryEntry.put("healthStatus", snap.getHealthStatus() != null ? snap.getHealthStatus() : "Healthy");
        diaryEntry.put("healthPercentage", snap.getHealthPercentage());
        diaryEntry.put("healthColor", snap.getHealthColor() != null ? snap.getHealthColor() : "#81C784");
        diaryEntry.put("matchConfidencePercentage", snap.getMatchConfidencePercentage());
        diaryEntry.put("careDifficultyText", snap.getCareDifficultyText() != null ? snap.getCareDifficultyText() : "Easy");
        diaryEntry.put("careDifficultyPercentage", snap.getCareDifficultyPercentage());
        diaryEntry.put("leafColors", snap.getLeafColors() != null ? snap.getLeafColors() : new ArrayList<String>());
        diaryEntry.put("aliases", snap.getAliases() != null ? snap.getAliases() : "N/A");
        diaryEntry.put("petToxicity", snap.getPetToxicity() != null ? snap.getPetToxicity() : "Non-toxic");
        diaryEntry.put("weedPotential", snap.getWeedPotential() != null ? snap.getWeedPotential() : "Low");
        diaryEntry.put("distribution", snap.getDistribution() != null ? snap.getDistribution() : "N/A");
        diaryEntry.put("habitat", snap.getHabitat() != null ? snap.getHabitat() : "N/A");
        diaryEntry.put("plantType", snap.getPlantType() != null ? snap.getPlantType() : "Unknown");
        diaryEntry.put("lifespan", snap.getLifespan() != null ? snap.getLifespan() : "N/A");
        diaryEntry.put("isArtificial", snap.isArtificial());

        diaryEntry.put("ultimateHeight", snap.getUltimateHeight() != null ? snap.getUltimateHeight() : "N/A");
        diaryEntry.put("ultimateSpread", snap.getUltimateSpread() != null ? snap.getUltimateSpread() : "N/A");
        diaryEntry.put("leafType", snap.getLeafType() != null ? snap.getLeafType() : "N/A");
        diaryEntry.put("plantingTime", snap.getPlantingTime() != null ? snap.getPlantingTime() : "N/A");
        diaryEntry.put("temperatureRange", snap.getTemperatureRange() != null ? snap.getTemperatureRange() : "N/A");
        diaryEntry.put("hardinessZones", snap.getHardinessZones() != null ? snap.getHardinessZones() : "N/A");
        diaryEntry.put("sunlight", snap.getSunlight() != null ? snap.getSunlight() : "Partial sun");
        diaryEntry.put("soil", snap.getSoil() != null ? snap.getSoil() : "Loam, Sandy loam");
        diaryEntry.put("pruning", snap.getPruning() != null ? snap.getPruning() : "N/A");
        diaryEntry.put("propagation", snap.getPropagation() != null ? snap.getPropagation() : "N/A");
        diaryEntry.put("repotting", snap.getRepotting() != null ? snap.getRepotting() : "N/A");

        diaryEntry.put("usesText", snap.getUsesText() != null ? snap.getUsesText() : "N/A");
        diaryEntry.put("adaptationText", snap.getAdaptationText() != null ? snap.getAdaptationText() : "N/A");
        diaryEntry.put("ecologicalText", snap.getEcologicalText() != null ? snap.getEcologicalText() : "N/A");
        diaryEntry.put("historyText", snap.getHistoryText() != null ? snap.getHistoryText() : "N/A");
        diaryEntry.put("nameStoryText", snap.getNameStoryText() != null ? snap.getNameStoryText() : "N/A");
        diaryEntry.put("symbolismText", snap.getSymbolismText() != null ? snap.getSymbolismText() : "N/A");

        diaryEntry.put("timestamp", System.currentTimeMillis());

        if (snap.getImageBase64() != null) {
            diaryEntry.put("imageBase64", snap.getImageBase64());
        }
        if (snap.getRawAnalysisJson() != null) {
            diaryEntry.put("rawAnalysisJson", snap.getRawAnalysisJson());
        }

        db.collection("diary")
                .document(docId)
                .set(diaryEntry)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Added " + snap.getPlantName() + " to My Garden!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to add plant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTopPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.inflate(R.menu.garden_top_options_menu);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_create_collection) {
                Toast.makeText(requireContext(), "Create Collection clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.action_sort) {
                Toast.makeText(requireContext(), "Sort clicked", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void switchToMyGarden() {
        if (tabMyGarden != null) tabMyGarden.setBackgroundResource(R.drawable.bg_active_pill);
        if (tabSnapHistory != null) tabSnapHistory.setBackgroundColor(Color.TRANSPARENT);

        if (tvTabMyGarden != null) tvTabMyGarden.setTextColor(COLOR_ACTIVE);
        if (tvTabSnapHistory != null) tvTabSnapHistory.setTextColor(COLOR_INACTIVE);

        if (layoutMyGardenContent != null) layoutMyGardenContent.setVisibility(View.VISIBLE);
        if (rvPlantList != null) rvPlantList.setVisibility(View.VISIBLE);

        if (layoutSnapHistoryContent != null) layoutSnapHistoryContent.setVisibility(View.GONE);
        if (rvSnapHistoryList != null) rvSnapHistoryList.setVisibility(View.GONE);
    }

    private void switchToSnapHistory() {
        if (tabMyGarden != null) tabMyGarden.setBackgroundColor(Color.TRANSPARENT);
        if (tabSnapHistory != null) tabSnapHistory.setBackgroundResource(R.drawable.bg_active_pill);

        if (tvTabMyGarden != null) tvTabMyGarden.setTextColor(COLOR_INACTIVE);
        if (tvTabSnapHistory != null) tvTabSnapHistory.setTextColor(COLOR_ACTIVE);

        if (layoutMyGardenContent != null) layoutMyGardenContent.setVisibility(View.GONE);
        if (rvPlantList != null) rvPlantList.setVisibility(View.GONE);

        if (layoutSnapHistoryContent != null) layoutSnapHistoryContent.setVisibility(View.VISIBLE);
        if (rvSnapHistoryList != null) rvSnapHistoryList.setVisibility(View.VISIBLE);
    }
}