package com.example.smartgrow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);

        if (mAuth.getCurrentUser() != null && prefManager != null && prefManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        } else if (mAuth.getCurrentUser() != null && (prefManager == null || !prefManager.isLoggedIn())) {
            mAuth.signOut();
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String input = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username/email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Signing in...");

            if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                loginWithEmailAndPassword(input, password, btnLogin);
            } else {
                findEmailByUsername(input, (foundEmail) -> {
                    if (foundEmail != null) {
                        loginWithEmailAndPassword(foundEmail, password, btnLogin);
                    } else {
                        resetButton(btnLogin);
                        Toast.makeText(LoginActivity.this, "Username does not exist", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        tvForgotPassword.setOnClickListener(v -> showResetPasswordDialog());
        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterStep1Activity.class)));
    }

    private void loginWithEmailAndPassword(String email, String password, MaterialButton btnLogin) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchUserProfile(mAuth.getCurrentUser().getUid(), btnLogin);
                    } else {
                        resetButton(btnLogin);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserProfile(String uid, MaterialButton btnLogin) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    resetButton(btnLogin);
                    if (doc.exists()) {
                        User user = new User();
                        user.setUid(uid);

                        String encUsername = doc.getString("username");
                        String encEmail = doc.getString("email");
                        String encFullName = doc.getString("fullName");
                        String encAddress = doc.getString("address");
                        String encPhone = doc.getString("phone");

                        user.setUsername(encUsername != null && !encUsername.isEmpty() ? FirebaseCryptoUtils.decrypt(encUsername, uid) : "");
                        user.setEmail(encEmail != null && !encEmail.isEmpty() ? FirebaseCryptoUtils.decrypt(encEmail, uid) : "");
                        user.setFullName(encFullName != null && !encFullName.isEmpty() ? FirebaseCryptoUtils.decrypt(encFullName, uid) : "");
                        user.setAddress(encAddress != null && !encAddress.isEmpty() ? FirebaseCryptoUtils.decrypt(encAddress, uid) : "");
                        user.setPhone(encPhone != null && !encPhone.isEmpty() ? FirebaseCryptoUtils.decrypt(encPhone, uid) : "");

                        user.setProfilePic(doc.getString("profilePic"));
                        user.setChoice1(doc.getString("choice1"));
                        user.setChoice2(doc.getString("choice2"));
                        user.setChoice3(doc.getString("choice3"));
                        user.setChoice4(doc.getString("choice4"));

                        SharedPrefManager prefManager = SharedPrefManager.getInstance(LoginActivity.this);
                        if (prefManager != null) {
                            prefManager.saveUser(user);
                        }

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    resetButton(btnLogin);
                    Toast.makeText(LoginActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private interface OnEmailFoundListener {
        void onResult(String email);
    }

    private void findEmailByUsername(String targetUsername, OnEmailFoundListener listener) {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docUid = doc.getId();
                        String encUsername = doc.getString("username");
                        if (encUsername != null && !encUsername.isEmpty()) {
                            String decryptedUsername = FirebaseCryptoUtils.decrypt(encUsername, docUid);
                            if (targetUsername.equalsIgnoreCase(decryptedUsername)) {
                                String encEmail = doc.getString("email");
                                String decryptedEmail = encEmail != null && !encEmail.isEmpty() ? FirebaseCryptoUtils.decrypt(encEmail, docUid) : null;
                                listener.onResult(decryptedEmail);
                                return;
                            }
                        }
                    }
                    listener.onResult(null);
                })
                .addOnFailureListener(e -> listener.onResult(null));
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your email or username");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        String prefilledText = etUsername.getText().toString().trim();
        if (!prefilledText.isEmpty()) {
            input.setText(prefilledText);
        }

        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String target = input.getText().toString().trim();
            if (target.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email or username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                sendResetEmail(target);
            } else {
                findEmailByUsername(target, email -> {
                    if (email != null && !email.isEmpty()) {
                        sendResetEmail(email);
                    } else {
                        Toast.makeText(LoginActivity.this, "Username not found.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset link sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetButton(MaterialButton btn) {
        btn.setEnabled(true);
        btn.setText("Sign in");
    }
}