package com.example.smartgrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 Full Screen Layout Setup (No Limits Background)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
            android.view.Window window = getWindow();
            window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }
        setContentView(R.layout.activity_login);

        // 🔗 Binding UI Elements
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // 🔓 1. LOGIN BUTTON CLICK (CONNECTED NA SA MAINACTIVITY)
        btnLogin.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Signing in...", Toast.LENGTH_SHORT).show();

            // HINT FOR ALLEN/LHEAN: Dito niyo isasaksak 'yung if-else validation mula sa API database niyo mamaya.
            // Sa ngayon, dadaan muna siya diretso sa dashboard para sa UI testing niyo.
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);

            // 🛑 Napakahalaga nito para kapag nasa Home Dashboard na si user,
            // kapag pinindot niya ang back button ng phone, HINDI na siya babalik sa Login screen.
            finish();
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