package com.example.smartgrow.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BiometricSecurityFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPrefManager prefManager;
    private SwitchCompat switchBiometric;

    public BiometricSecurityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_biometric_security, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        prefManager = SharedPrefManager.getInstance(requireContext());

        view.findViewById(R.id.btn_back_biometric).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        switchBiometric = view.findViewById(R.id.switch_biometric);

        checkBiometricSupport();
        loadBiometricStatus();

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateBiometricStatus(isChecked);
        });

        return view;
    }

    private void checkBiometricSupport() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                switchBiometric.setEnabled(true);
                break;
            default:
                switchBiometric.setEnabled(false);
                switchBiometric.setChecked(false);
                if (prefManager != null) prefManager.setBiometricEnabled(false);
                Toast.makeText(getContext(), "Biometric features are not available or not set up on this device", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void loadBiometricStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // First check local preference for immediate UI response
            if (prefManager != null) {
                switchBiometric.setChecked(prefManager.isBiometricEnabled());
            }

            // Then sync with Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isBiometricEnabled = documentSnapshot.getBoolean("isBiometricEnabled");
                            boolean enabled = isBiometricEnabled != null && isBiometricEnabled;
                            switchBiometric.setChecked(enabled);
                            if (prefManager != null) prefManager.setBiometricEnabled(enabled);
                        }
                    });
        }
    }

    private void updateBiometricStatus(boolean isEnabled) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isBiometricEnabled", isEnabled);

            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (prefManager != null) prefManager.setBiometricEnabled(isEnabled);
                        String msg = isEnabled ? "Biometric login enabled" : "Biometric login disabled";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update biometric settings", Toast.LENGTH_SHORT).show();
                        switchBiometric.setChecked(!isEnabled);
                    });
        }
    }
}
