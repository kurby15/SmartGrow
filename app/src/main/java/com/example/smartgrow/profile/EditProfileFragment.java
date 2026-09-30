package com.example.smartgrow.profile;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.button.MaterialButton;

// Modern Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private EditText etFullName, etEmail, etAddress, etPhone;
    private MaterialButton btnSave;

    // Firebase Auth & Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;

    private String currentUid;
    private String currentUsername;
    private SharedPrefManager prefManager;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = SharedPrefManager.getInstance(requireContext());

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }
        currentUsername = prefManager.getUsername();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Bind views
        etFullName = view.findViewById(R.id.et_fullname);
        etEmail = view.findViewById(R.id.et_email);
        etAddress = view.findViewById(R.id.et_address);
        etPhone = view.findViewById(R.id.et_phone);
        btnSave = view.findViewById(R.id.btn_save_edit_info);

        // Back button listener
        view.findViewById(R.id.btn_back_edit_info).setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (currentUid != null) {
            fetchUserData();
        } else {
            Toast.makeText(getContext(), "User session not found.", Toast.LENGTH_SHORT).show();
        }

        btnSave.setOnClickListener(v -> saveUserData());

        return view;
    }

    private void fetchUserData() {
        String docKey = (currentUsername != null && !currentUsername.trim().isEmpty() && !currentUsername.equals("unknown"))
                ? currentUsername
                : currentUid;

        userDocRef = db.collection("users").document(docKey);

        userDocRef.get().addOnSuccessListener(snapshot -> {
            if (isAdded() && snapshot.exists()) {
                populateFields(snapshot);
            } else if (isAdded()) {
                // Fallback query by UID if document key wasn't username
                db.collection("users").whereEqualTo("uid", currentUid).get().addOnSuccessListener(querySnapshot -> {
                    if (isAdded() && !querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        userDocRef = doc.getReference();
                        populateFields(doc);
                    }
                });
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(DocumentSnapshot snapshot) {
        // DECRYPT ALL SENSITIVE FIELDS
        String encFullName = snapshot.getString("fullName");
        String encEmail = snapshot.getString("email");
        String encAddress = snapshot.getString("address");
        String encPhone = snapshot.getString("phone");

        if (etFullName != null && encFullName != null) {
            etFullName.setText(FirebaseCryptoUtils.decrypt(encFullName, currentUid));
        }

        // Decrypt email if encrypted; fallback to raw value if unencrypted
        if (etEmail != null && encEmail != null) {
            String decryptedEmail = FirebaseCryptoUtils.decrypt(encEmail, currentUid);
            // Fallback check in case older users stored plain text emails
            if (decryptedEmail != null && !decryptedEmail.isEmpty()) {
                etEmail.setText(decryptedEmail);
            } else {
                etEmail.setText(encEmail);
            }
        }

        if (etAddress != null && encAddress != null) {
            etAddress.setText(FirebaseCryptoUtils.decrypt(encAddress, currentUid));
        }
        if (etPhone != null && encPhone != null) {
            etPhone.setText(FirebaseCryptoUtils.decrypt(encPhone, currentUid));
        }
    }

    private void saveUserData() {
        if (currentUid == null || userDocRef == null) {
            Toast.makeText(getContext(), "Unable to save: Invalid user session", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Basic Validations
        if (name.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Lock UI during process
        setFormEnabled(false);

        // ENCRYPT ALL SENSITIVE FIELDS BEFORE SAVING TO FIRESTORE
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", FirebaseCryptoUtils.encrypt(name, currentUid));
        updates.put("email", FirebaseCryptoUtils.encrypt(email, currentUid));
        updates.put("address", FirebaseCryptoUtils.encrypt(address, currentUid));
        updates.put("phone", FirebaseCryptoUtils.encrypt(phone, currentUid));

        userDocRef.update(updates).addOnCompleteListener(task -> {
            if (isAdded()) {
                setFormEnabled(true);

                if (task.isSuccessful()) {
                    // Update Local Preferences with unencrypted plain text data
                    User currentUser = prefManager.getUser();
                    if (currentUser == null) {
                        currentUser = new User();
                    }
                    currentUser.setUid(currentUid);
                    currentUser.setUsername(currentUsername);
                    currentUser.setFullName(name);
                    currentUser.setEmail(email);
                    currentUser.setAddress(address);
                    currentUser.setPhone(phone);

                    prefManager.saveUser(currentUser);

                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Update failed.";
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Helper to enable/disable form interactions during submission.
     */
    private void setFormEnabled(boolean enabled) {
        btnSave.setEnabled(enabled);
        btnSave.setText(enabled ? "Save" : "Saving...");
        etFullName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etAddress.setEnabled(enabled);
        etPhone.setEnabled(enabled);
    }
}