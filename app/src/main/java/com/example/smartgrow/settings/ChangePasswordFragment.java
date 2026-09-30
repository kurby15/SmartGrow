package com.example.smartgrow.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.smartgrow.R;
import com.example.smartgrow.auth.LoginActivity;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnSave;
    private FirebaseAuth mAuth;

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
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

        View btnBack = view.findViewById(R.id.btn_back_change_pass);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) getParentFragmentManager().popBackStack();
            });
        }

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

        if (currentPass.equals(newPass)) {
            Toast.makeText(getContext(), "New password cannot be the same as your current password.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(getContext(), "Session error: User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Verifying...");

        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), currentPass))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                
                                // Sign out and clear local session
                                mAuth.signOut();
                                if (isAdded() && getContext() != null) {
                                    SharedPrefManager.getInstance(requireContext()).logout(requireContext());
                                    
                                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    requireActivity().finish();
                                }
                            } else {
                                btnSave.setEnabled(true);
                                btnSave.setText("Update Password");
                                String errMsg = updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed";
                                Toast.makeText(getContext(), "Failed to update password: " + errMsg, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("Update Password");
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Incorrect password";
                        Toast.makeText(getContext(), "Current password incorrect: " + errMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
