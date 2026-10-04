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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userEmail;

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

        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFormFields();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etNewPassword.addTextChangedListener(passwordWatcher);
        etConfirmPassword.addTextChangedListener(passwordWatcher);

        btnUpdatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
                    Toast.makeText(ResetPasswordActivity.this, "Password must be 8-12 characters, containing an uppercase letter, number, and special character.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(ResetPasswordActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchOldPasswordAndProcess(newPassword);
            }
        });
    }

    private void validateFormFields() {
        String newPwd = etNewPassword.getText().toString().trim();
        String confirmPwd = etConfirmPassword.getText().toString().trim();
        boolean isValid = !newPwd.isEmpty() && !confirmPwd.isEmpty() && newPwd.equals(confirmPwd);
        btnUpdatePassword.setAlpha(isValid ? 1.0f : 0.6f);
    }

    private void fetchOldPasswordAndProcess(String newPassword) {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Email destination lost. Restart process.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Verifying...");

        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String foundOldPassword = null;
                for (DocumentSnapshot doc : task.getResult()) {
                    String uid = doc.getId();
                    String encEmail = doc.getString("email");
                    if (encEmail != null) {
                        String decryptedEmail = FirebaseCryptoUtils.decrypt(encEmail, uid);
                        if (userEmail.equalsIgnoreCase(decryptedEmail)) {
                            String encPassword = doc.getString("password");
                            foundOldPassword = FirebaseCryptoUtils.decrypt(encPassword, uid);
                            break;
                        }
                    }
                }

                if (foundOldPassword != null) {
                    if (foundOldPassword.equals(newPassword)) {
                        resetButtonState();
                        Toast.makeText(this, "The new password cannot be the same as your current password.", Toast.LENGTH_SHORT).show();
                    } else {
                        performPasswordUpdate(foundOldPassword, newPassword);
                    }
                } else {
                    resetButtonState();
                    Toast.makeText(this, "User record not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                resetButtonState();
                Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performPasswordUpdate(String oldPassword, String newPassword) {
        btnUpdatePassword.setText("Updating...");

        mAuth.signInWithEmailAndPassword(userEmail, oldPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        mAuth.getCurrentUser().updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        saveNewPasswordToFirestore(uid, newPassword);
                                    } else {
                                        resetButtonState();
                                        Toast.makeText(ResetPasswordActivity.this, "Failed to update security credentials.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        resetButtonState();
                        Toast.makeText(ResetPasswordActivity.this, "Authentication failed during reset.", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(ResetPasswordActivity.this, "Database synchronization failed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetButtonState() {
        btnUpdatePassword.setEnabled(true);
        btnUpdatePassword.setText("Reset Password");
    }
}