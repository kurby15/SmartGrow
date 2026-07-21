package com.example.smartgrow;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChangePasswordFragment extends Fragment {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnSave;
    private DatabaseReference userRef;
    private String currentUsername;

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUsername = SharedPrefManager.getInstance(requireContext()).getUsername();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        // Bindings
        etCurrentPassword = view.findViewById(R.id.et_current_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        btnSave = view.findViewById(R.id.btn_save_password);

        view.findViewById(R.id.btn_back_change_pass).setOnClickListener(v -> {
            if (isAdded()) getParentFragmentManager().popBackStack();
        });

        btnSave.setOnClickListener(v -> validateAndChangePassword());

        return view;
    }

    private void validateAndChangePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 8) {
            Toast.makeText(getContext(), "New password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Updating...");

        // 🔐 Security Flow: Fetch, Verify, Hash, and Update
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // 1. Verify Current Password (Confidentiality)
                        if (SecurityUtils.verifyPassword(currentPass, user.getPassword())) {
                            
                            // 2. Hash New Password (Integrity)
                            String hashedPass = SecurityUtils.hashPassword(newPass);
                            
                            // 3. Update Database
                            userRef.child("password").setValue(hashedPass).addOnCompleteListener(task -> {
                                btnSave.setEnabled(true);
                                btnSave.setText("Save");
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                } else {
                                    Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save");
                            Toast.makeText(getContext(), "Incorrect current password", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
            }
        });
    }
}
