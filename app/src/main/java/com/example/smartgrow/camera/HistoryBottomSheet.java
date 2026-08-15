package com.example.smartgrow.camera;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HistoryBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvHistory;
    private TextView tvEmptyHistory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OnSessionSelectedListener listener;

    public interface OnSessionSelectedListener {
        void onSessionSelected(ChatSessionModel session);
    }

    public static HistoryBottomSheet newInstance() {
        return new HistoryBottomSheet();
    }

    public void setOnSessionSelectedListener(OnSessionSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations =
                    com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_chat_history_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvHistory = view.findViewById(R.id.rv_past_conversations);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHistory.setHasFixedSize(true);
        }

        loadHistoryFromFirestore();
    }

    private void loadHistoryFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Safety check if fragment was closed during network call
                    if (!isAdded() || getContext() == null) return;

                    List<ChatSessionModel> sessionList = new ArrayList<>();

                    if (documentSnapshot.exists() && documentSnapshot.contains("ai_history")) {
                        // Extract the map field directly from the user document
                        Map<String, Object> rawHistoryMap = (Map<String, Object>) documentSnapshot.get("ai_history");

                        if (rawHistoryMap != null) {
                            for (Map.Entry<String, Object> entry : rawHistoryMap.entrySet()) {
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> sessionData = (Map<String, Object>) entry.getValue();

                                    String sessionId = (String) sessionData.get("sessionId");
                                    if (sessionId == null || sessionId.isEmpty()) {
                                        sessionId = entry.getKey(); // Fallback to map key if missing
                                    }

                                    String title = (String) sessionData.get("title");
                                    Long timestamp = (Long) sessionData.get("timestamp");

                                    sessionList.add(new ChatSessionModel(
                                            sessionId,
                                            title != null ? title : "Untitled Session",
                                            timestamp != null ? timestamp : 0L
                                    ));
                                }
                            }
                        }
                    }

                    // Sort session list by newest timestamp first
                    Collections.sort(sessionList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    // Handle empty state toggle
                    if (sessionList.isEmpty()) {
                        if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.VISIBLE);
                        if (rvHistory != null) rvHistory.setVisibility(View.GONE);
                    } else {
                        if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.GONE);
                        if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
                    }

                    AiHistoryAdapter adapter = new AiHistoryAdapter(sessionList, session -> {
                        if (listener != null) {
                            listener.onSessionSelected(session);
                        }
                        dismiss();
                    });

                    if (rvHistory != null) {
                        rvHistory.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load chat history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent memory leaks
        listener = null;
    }
}