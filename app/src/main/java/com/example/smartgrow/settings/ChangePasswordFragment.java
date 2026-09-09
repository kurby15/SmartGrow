package com.example.smartgrow.settings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SecurityUtils;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

        // I-check muna kung may nakuhang username galing sa SharedPrefManager
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(getContext(), "Session error: Username not found. Please re-login.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Verifying...");

        // Siguraduhing tama ang reference node gamit ang kasalukuyang username
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Siguraduhing maibabalik agad ang button state
                btnSave.setEnabled(true);
                btnSave.setText("Update Password");

                if (snapshot.exists()) {
                    String storedPassword = snapshot.child("password").getValue(String.class);

                    if (storedPassword != null) {
                        if (SecurityUtils.verifyPassword(currentPass, storedPassword)) {
                            showOtpDialog(newPass);
                        } else {
                            Toast.makeText(getContext(), "Incorrect current password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Password field missing in database", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "User account not found in database (" + currentUsername + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Babalik sa dati ang button sakaling ma-cancel o ma-block ang query
                btnSave.setEnabled(true);
                btnSave.setText("Update Password");
                Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showOtpDialog(String newPass) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_otp, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(dialogView);

        EditText etOtpCode = dialogView.findViewById(R.id.et_otp_code);
        TextView tvResend = dialogView.findViewById(R.id.tv_otp_resend_action);
        MaterialButton btnVerifyOtp = dialogView.findViewById(R.id.btn_otp_verify);

        btnVerifyOtp.setOnClickListener(v -> {
            String otpCode = etOtpCode.getText().toString().trim();

            if (otpCode.length() < 6) {
                etOtpCode.setError("Enter the 6-digit code");
                return;
            }

            btnVerifyOtp.setEnabled(false);
            btnVerifyOtp.setText("Verifying...");

            // 3. Hash New Password and Update Firebase Database
            String hashedPass = SecurityUtils.hashPassword(newPass);
            userRef.child("password").setValue(hashedPass).addOnCompleteListener(task -> {
                bottomSheetDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvResend.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Verification code resent!", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }
}