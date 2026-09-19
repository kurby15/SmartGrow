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
import com.example.smartgrow.plants.MyGardenFragment;
import com.example.smartgrow.plants.PlantNameBottomSheetFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

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
                // This fragment usually just shows history, but we can add save logic if needed
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