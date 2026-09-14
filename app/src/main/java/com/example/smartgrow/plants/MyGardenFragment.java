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
import android.widget.ImageView;
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

    // Stats Summary Elements
    private TextView tvStatPlantsCount, tvStatScannedCount, tvStatToWaterCount;

    // Content Containers & RecyclerViews
    private View layoutMyGardenContent, layoutSnapHistoryContent;
    private RecyclerView rvPlantList, rvSnapHistoryList;
    private LinearLayout layoutEmptyPlants;

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

    private boolean isDiaryLoaded = false;

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

        tvStatPlantsCount = view.findViewById(R.id.tv_stat_plants_count);
        tvStatScannedCount = view.findViewById(R.id.tv_stat_scanned_count);
        tvStatToWaterCount = view.findViewById(R.id.tv_stat_to_water_count);

        layoutMyGardenContent = view.findViewById(R.id.layout_my_garden_content);
        layoutSnapHistoryContent = view.findViewById(R.id.layout_snap_history_content);

        rvPlantList = view.findViewById(R.id.rv_plant_list);
        rvSnapHistoryList = view.findViewById(R.id.rv_snap_history_list);

        layoutEmptyPlants = view.findViewById(R.id.layout_empty_plants);

        ibTopMore = view.findViewById(R.id.ib_filter_more);

        // Bind single shared search EditText from XML
        etSearchPlants = view.findViewById(R.id.et_search_plants);

        layoutViewAll = view.findViewById(R.id.layout_view_all);

        layoutViewAll.setOnClickListener(v -> {
            AllPlantsFragment allPlantsFragment = new AllPlantsFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, allPlantsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Default "To Water" to 0
        if (tvStatToWaterCount != null) tvStatToWaterCount.setText("0");
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

        boolean isScanHistoryVisible = (layoutSnapHistoryContent != null && layoutSnapHistoryContent.getVisibility() == View.VISIBLE);

        if (!isScanHistoryVisible) {
            // Filter My Garden Tab lang
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
        } else {
            // Filter Scan History Tab lang
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
    }

    private void setupRecyclerViews() {
        // --- 1. My Garden Adapter ---
        gardenAdapter = new MyGardenPlantAdapter(plantList, new MyGardenPlantAdapter.OnPlantClickListener() {
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

    private void updateEmptyState() {

        if (!isDiaryLoaded) {
            if (rvPlantList != null) rvPlantList.setVisibility(View.GONE);
            if (layoutEmptyPlants != null) layoutEmptyPlants.setVisibility(View.GONE);
            return;
        }

        if (plantList.isEmpty()) {
            if (rvPlantList != null) rvPlantList.setVisibility(View.GONE);
            if (layoutEmptyPlants != null) layoutEmptyPlants.setVisibility(View.VISIBLE);
        } else {
            if (rvPlantList != null) rvPlantList.setVisibility(View.VISIBLE);
            if (layoutEmptyPlants != null) layoutEmptyPlants.setVisibility(View.GONE);
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

    private void listenToDiaryData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            isDiaryLoaded = true; // Mark as loaded even if empty
            plantList.clear();
            fullPlantList.clear();
            if (gardenAdapter != null) gardenAdapter.notifyDataSetChanged();
            if (snapAdapter != null) snapAdapter.setGardenPlantNames(new ArrayList<>());
            updateEmptyState();
            updateStatsCounts();
            return;
        }

        diaryListener = db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    isDiaryLoaded = true; // Nakuha na ang unang sagot mula sa Firestore

                    if (error != null) {
                        Log.e("FirestoreError", "Error listening to diary data", error);
                        updateEmptyState();
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

                        boolean isGardenVisible = (layoutMyGardenContent != null && layoutMyGardenContent.getVisibility() == View.VISIBLE);
                        if (isGardenVisible) {
                            if (etSearchPlants != null && etSearchPlants.getText() != null && !etSearchPlants.getText().toString().isEmpty()) {
                                filterLists(etSearchPlants.getText().toString());
                            } else {
                                plantList.clear();
                                int limit = Math.min(fullPlantList.size(), 4);
                                plantList.addAll(fullPlantList.subList(0, limit));
                                if (gardenAdapter != null) {
                                    gardenAdapter.notifyDataSetChanged();
                                }
                            }
                        }

                        updateEmptyState();
                        updateStatsCounts();

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
            updateStatsCounts();
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
                        updateStatsCounts();
                    }
                });
    }

    private void updateStatsCounts() {
        if (tvStatPlantsCount != null) {
            tvStatPlantsCount.setText(String.valueOf(fullPlantList.size()));
        }
        if (tvStatScannedCount != null) {
            tvStatScannedCount.setText(String.valueOf(fullSnapList.size()));
        }
        // "To Water" count logic - based on health percentage < 60 like in HomeFragment
        int toWaterCount = 0;
        for (MyGardenPlantModel plant : fullPlantList) {
            if (plant.getHealthPercentage() < 60) {
                toWaterCount++;
            }
        }
        if (tvStatToWaterCount != null) {
            tvStatToWaterCount.setText(String.valueOf(toWaterCount));
        }
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
        if (tvTabMyGarden != null) tvTabMyGarden.setTextColor(COLOR_ACTIVE);

        if (tabSnapHistory != null) tabSnapHistory.setBackgroundResource(R.drawable.bg_inactive_pill);
        if (tvTabSnapHistory != null) tvTabSnapHistory.setTextColor(COLOR_INACTIVE);

        updateTabIcons(true);

        if (layoutMyGardenContent != null) layoutMyGardenContent.setVisibility(View.VISIBLE);

        plantList.clear();
        int limit = Math.min(fullPlantList.size(), 4);
        plantList.addAll(fullPlantList.subList(0, limit));
        if (gardenAdapter != null) {
            gardenAdapter.notifyDataSetChanged();
        }
        updateEmptyState();

        if (layoutSnapHistoryContent != null) layoutSnapHistoryContent.setVisibility(View.GONE);
    }

    private void switchToSnapHistory() {
        if (tabMyGarden != null) tabMyGarden.setBackgroundResource(R.drawable.bg_inactive_pill);
        if (tvTabMyGarden != null) tvTabMyGarden.setTextColor(COLOR_INACTIVE);

        if (tabSnapHistory != null) tabSnapHistory.setBackgroundResource(R.drawable.bg_active_pill);
        if (tvTabSnapHistory != null) tvTabSnapHistory.setTextColor(COLOR_ACTIVE);

        updateTabIcons(false);

        // Itago ang My Garden contents
        if (layoutMyGardenContent != null) layoutMyGardenContent.setVisibility(View.GONE);
        if (layoutEmptyPlants != null) layoutEmptyPlants.setVisibility(View.GONE);

        // Ipakita ang Scan History contents at i-load ang data
        if (layoutSnapHistoryContent != null) layoutSnapHistoryContent.setVisibility(View.VISIBLE);

        snapList.clear();
        snapList.addAll(fullSnapList);
        if (snapAdapter != null) {
            snapAdapter.notifyDataSetChanged();
        }
    }

    private void updateTabIcons(boolean isMyGardenActive) {
        if (tabMyGarden != null) {
            ImageView imgGarden = (ImageView) tabMyGarden.getChildAt(0);
            if (imgGarden != null) {
                imgGarden.setColorFilter(isMyGardenActive ? Color.parseColor("#FFFFFF") : Color.parseColor("#2E4336"));
            }
        }

        if (tabSnapHistory != null) {
            ImageView imgHistory = (ImageView) tabSnapHistory.getChildAt(0);
            if (imgHistory != null) {
                imgHistory.setColorFilter(isMyGardenActive ? Color.parseColor("#2E4336") : Color.parseColor("#FFFFFF"));
            }
        }
    }
}