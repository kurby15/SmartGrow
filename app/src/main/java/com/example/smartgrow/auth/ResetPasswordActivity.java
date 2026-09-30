package com.example.smartgrow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userEmail;

    // Strict validation regex matching your specified standard pattern requirements
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.{8,12}$)(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~£÷×]).*$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userEmail = getIntent().getStringExtra("email");
        
        setContentView(R.layout.activity_reset_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Views
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        // Real-time TextWatcher for immediate field validation handling
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFormFields();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etOldPassword.addTextChangedListener(passwordWatcher);
        etNewPassword.addTextChangedListener(passwordWatcher);
        etConfirmPassword.addTextChangedListener(passwordWatcher);

        btnUpdatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPassword = etOldPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (oldPassword.equals(newPassword)) {
                    Toast.makeText(ResetPasswordActivity.this, "The new password cannot be the same as your current password.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
                    Toast.makeText(ResetPasswordActivity.this, "Password must be 8-12 characters, containing an uppercase letter, number, and special character.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(ResetPasswordActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                performPasswordUpdate(oldPassword, newPassword);
            }
        });
    }

    private void validateFormFields() {
        String oldPwd = etOldPassword.getText().toString().trim();
        String newPwd = etNewPassword.getText().toString().trim();
        String confirmPwd = etConfirmPassword.getText().toString().trim();

        // Dynamically update update button visual availability based on completion
        boolean isValid = !oldPwd.isEmpty() && !newPwd.isEmpty() && !confirmPwd.isEmpty() && newPwd.equals(confirmPwd);
        btnUpdatePassword.setAlpha(isValid ? 1.0f : 0.6f);
    }

    private void performPasswordUpdate(String oldPassword, String newPassword) {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Email destination lost. Restart process.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // 1. Direct Re-authentication against Firebase Authentication (Source of Truth)
        mAuth.signInWithEmailAndPassword(userEmail, oldPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        
                        // 2. Core Auth password update to keep credentials fully live
                        mAuth.getCurrentUser().updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        // 3. Encrypt and save safely to Firestore so profile data remains secure & synced
                                        saveNewPasswordToFirestore(uid, newPassword);
                                    } else {
                                        resetButtonState();
                                        String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "Auth update failed";
                                        Toast.makeText(ResetPasswordActivity.this, "Failed to update security credentials: " + error, Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        resetButtonState();
                        String error = task.getException() != null ? task.getException().getMessage() : "Incorrect current password";
                        Toast.makeText(ResetPasswordActivity.this, "Current Password Incorrect: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveNewPasswordToFirestore(String uid, String newPassword) {
        String encryptedPassword = FirebaseCryptoUtils.encrypt(newPassword, uid);

        db.collection("users").document(uid)
                .update("password", encryptedPassword)
                .addOnSuccessListener(aVoid -> {
                    mAuth.signOut();
                    Toast.makeText(ResetPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                    
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButtonState();
                    Toast.makeText(ResetPasswordActivity.this, "Database synchronization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetButtonState() {
        btnUpdatePassword.setEnabled(true);
        btnUpdatePassword.setText("Update Password");
    }
}