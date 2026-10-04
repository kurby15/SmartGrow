package com.example.smartgrow.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.camera.PlantAnalyzer;
import com.example.smartgrow.core.NotificationHelper;
import com.example.smartgrow.plants.MyGardenFragment;
import com.example.smartgrow.plants.PlantNameBottomSheetFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapHistoryFragment extends Fragment {

    private RecyclerView rvSnapHistory;
    private SnapHistoryAdapter snapAdapter;
    private List<SnapHistoryModel> snapList;
    private List<SnapHistoryModel> fullSnapList;

    private EditText etSearchHistory;
    private ImageButton ibFilterSort;
    private TextView tvTabMyGarden;
    private MaterialCardView cardExpirationBanner;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_snap_history, container, false);

        rvSnapHistory = view.findViewById(R.id.rv_snap_history_list);
        etSearchHistory = view.findViewById(R.id.et_search_history);
        tvTabMyGarden = view.findViewById(R.id.tv_tab_my_garden);
        cardExpirationBanner = view.findViewById(R.id.card_expiration_banner);

        rvSnapHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        snapList = new ArrayList<>();
        fullSnapList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();

        if (tvTabMyGarden != null) {
            tvTabMyGarden.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new MyGardenFragment())
                            .commit();
                }
            });
        }

        setupSearchFilter();
        fetchSnapHistoryData();
        fetchGardenPlantNames();

        return view;
    }

    private void setupRecyclerView() {
        snapAdapter = new SnapHistoryAdapter(snapList, new SnapHistoryAdapter.OnSnapClickListener() {
            @Override
            public void onSnapClick(SnapHistoryModel snap) {
                // View details logic
            }

            @Override
            public void onAddToGardenClick(SnapHistoryModel snap) {
                if (snap != null) {
                    saveSnapToGarden(snap);
                }
            }

            @Override
            public void onEditNameClick(SnapHistoryModel snap) {
                List<String> suggestions = new ArrayList<>();
                if (snap.getRawAnalysisJson() != null) {
                    suggestions = PlantAnalyzer.extractPlantSuggestionsFromRawJson(snap.getRawAnalysisJson());
                }

                showNamePlantBottomSheet(snap.getPlantName(), suggestions, newName -> {
                    snap.setPlantName(newName);
                    if (snapAdapter != null) snapAdapter.notifyDataSetChanged();
                    updatePlantNameInFirestore(snap.getId(), newName);
                });
            }

            @Override
            public void onDeleteSnapClick(SnapHistoryModel snap) {
                if (snap != null && snap.getId() != null) {
                    db.collection("diary_history").document(snap.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                snapList.remove(snap);
                                fullSnapList.remove(snap);
                                snapAdapter.notifyDataSetChanged();
                                if (getContext() != null) Toast.makeText(getContext(), "Deleted from history", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });
        rvSnapHistory.setAdapter(snapAdapter);
    }

    private void saveSnapToGarden(SnapHistoryModel snap) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Login to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = String.valueOf(System.currentTimeMillis());
        Map<String, Object> diaryEntry = new HashMap<>();
        diaryEntry.put("id", docId);
        diaryEntry.put("userId", user.getUid());
        diaryEntry.put("plant_uid", snap.getPlantUid() != null ? snap.getPlantUid() : docId);
        diaryEntry.put("plantName", snap.getPlantName());
        diaryEntry.put("scientificName", snap.getScientificName());
        diaryEntry.put("healthStatus", snap.getHealthStatus());
        diaryEntry.put("healthPercentage", snap.getHealthPercentage());
        diaryEntry.put("healthColor", snap.getHealthColor());
        diaryEntry.put("matchConfidencePercentage", snap.getMatchConfidencePercentage());
        diaryEntry.put("aliases", snap.getAliases());
        diaryEntry.put("isArtificial", snap.isArtificial());
        diaryEntry.put("timestamp", System.currentTimeMillis());
        diaryEntry.put("rawAnalysisJson", snap.getRawAnalysisJson());
        diaryEntry.put("imageBase64", snap.getImageBase64());
        
        diaryEntry.put("distribution", snap.getDistribution());
        diaryEntry.put("habitat", snap.getHabitat());
        diaryEntry.put("petToxicity", snap.getPetToxicity());
        diaryEntry.put("weedPotential", snap.getWeedPotential());
        diaryEntry.put("plantType", snap.getPlantType());
        diaryEntry.put("lifespan", snap.getLifespan());
        diaryEntry.put("careDifficultyText", snap.getCareDifficultyText());

        // Automatic care reminders based on health percentage
        Map<String, String> schedule = getAutoSchedule(snap.getHealthPercentage());
        Map<String, Object> reminderData = new HashMap<>();
        reminderData.put("wateringSchedule", schedule.get("water"));
        reminderData.put("fertilizerSchedule", schedule.get("fertilizer"));
        reminderData.put("sunlightSchedule", schedule.get("sunlight"));
        reminderData.put("preferredTime", "08:00 AM");
        diaryEntry.put("reminders", reminderData);

        db.collection("diary").document(docId).set(diaryEntry)
            .addOnSuccessListener(aVoid -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Added to My Garden with automatic reminders!", Toast.LENGTH_SHORT).show();
                    
                    try {
                        NotificationHelper.createNotificationChannel(getContext());
                        NotificationHelper.scheduleReminder(getContext(), docId, snap.getPlantName(), "Water", "08:00 AM", schedule.get("water"));
                        NotificationHelper.scheduleReminder(getContext(), docId, snap.getPlantName(), "Fertilizer", "08:00 AM", schedule.get("fertilizer"));
                        NotificationHelper.scheduleReminder(getContext(), docId, snap.getPlantName(), "Sunlight", "08:00 AM", schedule.get("sunlight"));
                    } catch (Exception e) {
                        Log.e("SnapHistoryFragment", "Error scheduling reminders", e);
                    }
                    
                    fetchGardenPlantNames();
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private Map<String, String> getAutoSchedule(int health) {
        Map<String, String> schedule = new HashMap<>();
        if (health >= 80) {
            schedule.put("water", "Every 3 Days");
            schedule.put("fertilizer", "Every 2 Weeks");
            schedule.put("sunlight", "Every Day");
        } else if (health >= 50) {
            schedule.put("water", "Every 2 Days");
            schedule.put("fertilizer", "Every Week");
            schedule.put("sunlight", "Every Day");
        } else {
            schedule.put("water", "Every Day");
            schedule.put("fertilizer", "Every Week");
            schedule.put("sunlight", "Every Day");
        }
        return schedule;
    }

    private void fetchGardenPlantNames() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("diary")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> gardenNames = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("plantName");
                        if (name != null) gardenNames.add(name.trim());
                    }
                    if (snapAdapter != null) {
                        snapAdapter.setGardenPlantNames(gardenNames);
                    }
                });
    }

    private void showNamePlantBottomSheet(String currentName, List<String> suggestions, PlantNameBottomSheetFragment.OnPlantNameUpdatedListener updateListener) {
        PlantNameBottomSheetFragment bottomSheet = PlantNameBottomSheetFragment.newInstance(currentName, suggestions);
        bottomSheet.setOnPlantNameUpdatedListener(updateListener);
        bottomSheet.show(getParentFragmentManager(), "PlantNameBottomSheet");
    }

    private void updatePlantNameInFirestore(String documentId, String newName) {
        if (documentId == null || documentId.isEmpty()) return;

        db.collection("diary_history").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String plantUid = documentSnapshot.getString("plant_uid");
                        String userId = documentSnapshot.getString("userId");

                        WriteBatch batch = db.batch();
                        batch.update(db.collection("diary_history").document(documentId), "plantName", newName);

                        if (plantUid != null && !plantUid.isEmpty() && userId != null) {
                            // Sync with 'diary' (Garden)
                            db.collection("diary")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("plant_uid", plantUid)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        WriteBatch syncBatch = db.batch();
                                        for (QueryDocumentSnapshot doc : querySnapshot) {
                                            syncBatch.update(doc.getReference(), "plantName", newName);
                                        }

                                        // Sync with other 'diary_history' records
                                        db.collection("diary_history")
                                                .whereEqualTo("userId", userId)
                                                .whereEqualTo("plant_uid", plantUid)
                                                .get()
                                                .addOnSuccessListener(historySnapshot -> {
                                                    for (QueryDocumentSnapshot doc : historySnapshot) {
                                                        syncBatch.update(doc.getReference(), "plantName", newName);
                                                    }
                                                    syncBatch.commit().addOnSuccessListener(aVoid -> {
                                                        if (getContext() != null) Toast.makeText(getContext(), "Name updated everywhere!", Toast.LENGTH_SHORT).show();
                                                    });
                                                });
                                    });
                        } else {
                            batch.commit().addOnSuccessListener(aVoid -> {
                                if (getContext() != null) Toast.makeText(getContext(), "Renamed successfully", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
    }

    private void fetchSnapHistoryData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            snapList.clear();
            fullSnapList.clear();
            if (snapAdapter != null) snapAdapter.notifyDataSetChanged();
            return;
        }

        db.collection("diary_history")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    snapList.clear();
                    fullSnapList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        SnapHistoryModel snap = doc.toObject(SnapHistoryModel.class);
                        if (snap != null) {
                            if (snap.getId() == null || snap.getId().isEmpty()) {
                                snap.setId(doc.getId());
                            }
                            snapList.add(snap);
                            fullSnapList.add(snap);
                        }
                    }

                    if (etSearchHistory != null && !etSearchHistory.getText().toString().isEmpty()) {
                        filter(etSearchHistory.getText().toString());
                    } else if (snapAdapter != null) {
                        snapAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching snap history", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearchFilter() {
        if (etSearchHistory != null) {
            etSearchHistory.addTextChangedListener(new TextWatcher() {
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
        snapList.clear();
        if (query == null || query.trim().isEmpty()) {
            snapList.addAll(fullSnapList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (SnapHistoryModel item : fullSnapList) {
                boolean matchesCommonName = item.getPlantName() != null && item.getPlantName().toLowerCase().contains(lowerCaseQuery);
                boolean matchesScientificName = item.getScientificName() != null && item.getScientificName().toLowerCase().contains(lowerCaseQuery);

                if (matchesCommonName || matchesScientificName) {
                    snapList.add(item);
                }
            }
        }
        if (snapAdapter != null) {
            snapAdapter.notifyDataSetChanged();
        }
    }
}
