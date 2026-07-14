package com.example.smartgrow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        setContentView(R.layout.activity_login);

        // 🔗 Binding UI Elements
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // 🔓 1. LOGIN BUTTON CLICK (CONNECTED NA SA MAINACTIVITY)
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(LoginActivity.this, "Signing in...", Toast.LENGTH_SHORT).show();

            databaseReference.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getPassword().equals(password)) {
                            // Save login state and username
                            SharedPreferences preferences = getSharedPreferences("SmartGrowPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("current_username", username);
                            editor.putBoolean("is_logged_in", true);
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 🔑 2. FORGOT PASSWORD CLICK
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Opening Forgot Password Screen...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // 📝 3. GO TO REGISTER CLICK
        tvGoToRegister.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Please follow the steps to create an account", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, RegisterStep1Activity.class);
            startActivity(intent);
        });
    }
}