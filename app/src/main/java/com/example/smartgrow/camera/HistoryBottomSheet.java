package com.example.smartgrow.camera;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HistoryBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "HistoryBottomSheet";

    private RecyclerView rvHistory;
    private TextView tvEmptyHistory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OnSessionSelectedListener listener;
    private AiHistoryAdapter adapter;
    private List<ChatSessionModel> sessionList = new ArrayList<>();

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

        db.collection("ai_chat_messages")
                .whereEqualTo("uid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;

                    sessionList.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String sessionId = doc.getString("sessionId");
                            if (sessionId == null || sessionId.isEmpty()) {
                                sessionId = doc.getId();
                            }

                            Long timestamp = doc.getLong("lastUpdated");
                            if (timestamp == null) {
                                timestamp = doc.getLong("createdTimestamp");
                            }
                            if (timestamp == null) {
                                timestamp = 0L;
                            }

                            String title = extractSessionTitle(doc);

                            sessionList.add(new ChatSessionModel(
                                    sessionId,
                                    title,
                                    timestamp
                            ));
                        }
                    }

                    Collections.sort(sessionList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    updateUIState();

                    adapter = new AiHistoryAdapter(sessionList, session -> {
                        if (listener != null) {
                            listener.onSessionSelected(session);
                        }
                        dismiss();
                    }, (session, position) -> {
                        showDeleteConfirmation(session, position);
                    });

                    if (rvHistory != null) {
                        rvHistory.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching chat history", e);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load chat history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDeleteConfirmation(ChatSessionModel session, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete History")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSession(session, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSession(ChatSessionModel session, int position) {
        db.collection("ai_chat_messages")
                .document(session.getSessionId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    sessionList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateUIState();
                    Toast.makeText(getContext(), "Conversation deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIState() {
        if (sessionList.isEmpty()) {
            if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.VISIBLE);
            if (rvHistory != null) rvHistory.setVisibility(View.GONE);
        } else {
            if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.GONE);
            if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private String extractSessionTitle(DocumentSnapshot doc) {
        List<Map<String, Object>> messagesArray = (List<Map<String, Object>>) doc.get("messages");

        if (messagesArray != null && !messagesArray.isEmpty()) {
            for (Map<String, Object> msg : messagesArray) {
                Long typeObj = (Long) msg.get("messageType");
                int messageType = typeObj != null ? typeObj.intValue() : -1;

                String text = (String) msg.get("messageText");
                if (text != null && !text.trim().isEmpty()) {
                    if (messageType == ChatMessageModel.TYPE_USER || messageType == 2) {
                        String cleanTitle = text.trim();
                        if (cleanTitle.length() > 32) {
                            cleanTitle = cleanTitle.substring(0, 32) + "...";
                        }
                        return cleanTitle;
                    }
                }
            }

            for (Map<String, Object> msg : messagesArray) {
                String text = (String) msg.get("messageText");
                if (text != null && !text.trim().isEmpty()) {
                    String cleanTitle = text.trim();
                    if (cleanTitle.length() > 32) {
                        cleanTitle = cleanTitle.substring(0, 32) + "...";
                    }
                    return cleanTitle;
                }
            }
        }

        return "Conversation " + doc.getId();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}