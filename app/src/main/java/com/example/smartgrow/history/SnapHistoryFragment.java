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
import com.example.smartgrow.plants.MyGardenFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        snapAdapter = new SnapHistoryAdapter(snapList, null);
        rvSnapHistory.setAdapter(snapAdapter);

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

                    // 🔴 Re-apply active search query if user typed prior to fetch response
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