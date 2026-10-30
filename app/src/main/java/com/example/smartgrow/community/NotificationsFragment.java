package com.example.smartgrow.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private View layoutNoNotifications;
    private TextView btnClearAll;
    private View btnBack;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration notificationListener;

    public NotificationsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notification, container, false);

        rvNotifications = view.findViewById(R.id.rv_notifications_list);
        layoutNoNotifications = view.findViewById(R.id.layout_no_notifications);
        btnClearAll = view.findViewById(R.id.btn_clear_all_notifications);
        btnBack = view.findViewById(R.id.btn_close_notification);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);

        adapter.setOnNotificationClickListener(notification -> {
            if (notification.getPostId() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, CommunityForumFragment.newInstance(notification.getPostId()))
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        listenForNotifications();

        return view;
    }

    private void listenForNotifications() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        notificationListener = db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (!isAdded() || error != null || querySnapshot == null) return;

                    notificationList.clear();
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                        NotificationModel n = snap.toObject(NotificationModel.class);
                        if (n != null) {
                            notificationList.add(n);
                            if (!n.isRead()) {
                                batch.update(snap.getReference(), "read", true);
                            }
                        }
                    }

                    if (notificationList.isEmpty()) {
                        rvNotifications.setVisibility(View.GONE);
                        layoutNoNotifications.setVisibility(View.VISIBLE);
                        btnClearAll.setVisibility(View.GONE);
                    } else {
                        rvNotifications.setVisibility(View.VISIBLE);
                        layoutNoNotifications.setVisibility(View.GONE);
                        btnClearAll.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                    batch.commit();
                });

        btnClearAll.setOnClickListener(v -> {
            notificationList.clear();
            adapter.notifyDataSetChanged();
            rvNotifications.setVisibility(View.GONE);
            layoutNoNotifications.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.GONE);

            db.collection("notifications")
                    .whereEqualTo("receiverId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit().addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Notifications cleared.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
