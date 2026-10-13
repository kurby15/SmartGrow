package com.example.smartgrow.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.smartgrow.R;
import com.example.smartgrow.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PrivacyDataFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public PrivacyDataFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_data, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Back Button
        view.findViewById(R.id.btn_back_privacy_data).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // Private Account Toggle
        SwitchCompat switchPrivate = view.findViewById(R.id.switch_private_account);
        loadPrivateAccountStatus(switchPrivate);
        
        switchPrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updatePrivateAccountStatus(isChecked);
        });

        // Account Deletion Switch
        SwitchCompat switchDeletion = view.findViewById(R.id.switch_deletion);
        switchDeletion.setChecked(false);

        switchDeletion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showDeleteConfirmationDialog(switchDeletion);
            }
        });

        return view;
    }

    private void loadPrivateAccountStatus(SwitchCompat switchView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isPrivate = documentSnapshot.getBoolean("isPrivate");
                            if (isPrivate != null) {
                                switchView.setChecked(isPrivate);
                            }
                        }
                    });
        }
    }

    private void updatePrivateAccountStatus(boolean isPrivate) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isPrivate", isPrivate);
            
            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        String msg = isPrivate ? "Account is now private" : "Account is now public";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update privacy settings", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteConfirmationDialog(SwitchCompat switchView) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone and you will be logged out automatically.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount(switchView))
                .setNegativeButton("Cancel", (dialog, which) -> {
                    switchView.setChecked(false);
                })
                .setCancelable(false)
                .show();
    }

    private void deleteAccount(SwitchCompat switchView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // Delete from Firestore first (optional, but good practice)
            db.collection("users").document(uid).delete();
            
            // Delete Auth User
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                    logoutAndRedirect();
                } else {
                    Toast.makeText(getContext(), "For security reasons, please re-login before deleting your account.", Toast.LENGTH_LONG).show();
                    switchView.setChecked(false);
                    logoutAndRedirect();
                }
            });
        } else {
            logoutAndRedirect();
        }
    }

    private void logoutAndRedirect() {
        mAuth.signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
