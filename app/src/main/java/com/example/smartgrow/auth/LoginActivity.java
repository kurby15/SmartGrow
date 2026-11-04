package com.example.smartgrow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.example.smartgrow.core.SecurityLogger;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.profile.User;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvLockMessage;
    private MaterialButton btnLogin, btnBiometric;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefManager prefManager;
    private CountDownTimer lockoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        // Forced flow: Removed auto-login redirect to MainActivity.
        // Even if logged in, the user must pass through this screen.
        if (mAuth.getCurrentUser() != null && (prefManager == null || !prefManager.isLoggedIn())) {
            if (prefManager == null || !prefManager.isBiometricEnabled()) {
                mAuth.signOut();
            }
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvLockMessage = findViewById(R.id.tv_lock_message);
        btnLogin = findViewById(R.id.btn_login);
        btnBiometric = findViewById(R.id.btn_biometric_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // Pre-fill username/email for returning users to make the forced login flow smoother
        if (prefManager != null && prefManager.isLoggedIn()) {
            User savedUser = prefManager.getUser();
            if (savedUser != null) {
                String hint = savedUser.getEmail();
                if (hint == null || hint.isEmpty()) hint = savedUser.getUsername();
                if (hint != null && !hint.equals("unknown")) {
                    etUsername.setText(hint);
                }
            }
        }

        checkLockoutStatus();
        setupBiometricLogin();

        btnLogin.setOnClickListener(v -> {
            String input = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                SecurityLogger.log(SecurityLogger.SecurityEvent.INPUT_VALIDATION_FAILURE, "Empty login credentials submitted");
                Toast.makeText(this, "Please enter username/email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (prefManager.getRemainingLockoutMillis() > 0) {
                checkLockoutStatus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText(R.string.signing_in);

            if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                loginWithEmailAndPassword(input, password);
            } else {
                findEmailByUsername(input, (foundEmail) -> {
                    if (foundEmail != null) {
                        loginWithEmailAndPassword(foundEmail, password);
                    } else {
                        SecurityLogger.log(SecurityLogger.SecurityEvent.AUTH_FAILURE, "Username not found: " + input);
                        handleLoginFailure();
                    }
                });
            }
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterStep5Activity.class)));
    }

    private void setupBiometricLogin() {
        if (prefManager.isBiometricEnabled()) {
            BiometricManager biometricManager = BiometricManager.from(this);
            int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                btnBiometric.setVisibility(View.VISIBLE);
                btnBiometric.setOnClickListener(v -> showBiometricPrompt());
                
                // Automatically show biometric prompt if user previously enabled it
                new android.os.Handler().postDelayed(this::showBiometricPrompt, 600);
            } else {
                btnBiometric.setVisibility(View.GONE);
            }
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                User savedUser = prefManager.getUser();
                String savedPassword = prefManager.getPassword();
                
                if (savedUser != null && savedUser.getEmail() != null && savedPassword != null) {
                    loginWithEmailAndPassword(savedUser.getEmail(), savedPassword);
                } else if (mAuth.getCurrentUser() != null) {
                    fetchUserProfile(mAuth.getCurrentUser().getUid());
                } else {
                    Toast.makeText(LoginActivity.this, "Biometric login requires a previous successful password login.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use Password")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void checkLockoutStatus() {
        long remainingMillis = prefManager.getRemainingLockoutMillis();
        if (remainingMillis > 0) {
            startLockoutCountdown(remainingMillis);
        } else {
            if (lockoutTimer != null) {
                lockoutTimer.cancel();
            }
            tvLockMessage.setVisibility(View.GONE);
            resetButton(btnLogin);
        }
    }

    private void startLockoutCountdown(long millis) {
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
        }

        btnLogin.setEnabled(false);
        tvLockMessage.setVisibility(View.VISIBLE);

        lockoutTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeFormatted = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
                tvLockMessage.setText(getString(R.string.lockout_message, timeFormatted));
            }

            @Override
            public void onFinish() {
                tvLockMessage.setVisibility(View.GONE);
                resetButton(btnLogin);
                prefManager.resetLoginAttempts();
            }
        }.start();
    }

    private void handleLoginFailure() {
        resetButton(btnLogin);
        prefManager.incrementLoginAttempts();
        int attempts = prefManager.getLoginAttempts();

        if (attempts >= SharedPrefManager.MAX_LOGIN_ATTEMPTS) {
            checkLockoutStatus();
            Toast.makeText(this, R.string.lock_toast, Toast.LENGTH_LONG).show();
        } else {
            int remaining = SharedPrefManager.MAX_LOGIN_ATTEMPTS - attempts;
            Toast.makeText(this, getString(R.string.login_attempts_remaining, remaining), Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        prefManager.resetLoginAttempts();
                        if (prefManager.isBiometricEnabled()) {
                            prefManager.savePassword(password);
                        }
                        fetchUserProfile(mAuth.getCurrentUser().getUid());
                    } else {
                        handleLoginFailure();
                    }
                });
    }

    private void fetchUserProfile(String uid) {
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

                        if (prefManager != null) {
                            prefManager.saveUser(user);
                            Boolean biometricEnabled = doc.getBoolean("isBiometricEnabled");
                            prefManager.setBiometricEnabled(biometricEnabled != null && biometricEnabled);
                        }

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> resetButton(btnLogin));
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

    private void resetButton(MaterialButton btn) {
        if (prefManager.getRemainingLockoutMillis() <= 0) {
            btn.setEnabled(true);
            btn.setText(R.string.sign_in);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
        }
    }
}
