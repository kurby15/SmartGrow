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
import com.google.firebase.firestore.SetOptions;

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
        View btnBack = view.findViewById(R.id.btn_back_edit_info);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        if (currentUid != null) {
            fetchUserData();
        } else {
            Toast.makeText(getContext(), "User session not found.", Toast.LENGTH_SHORT).show();
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveUserData());
        }

        return view;
    }

    private void fetchUserData() {
        if (currentUid == null) return;

        // Try to find the document by username first (legacy), then fallback to UID
        String docKey = (currentUsername != null && !currentUsername.trim().isEmpty() && !currentUsername.equals("unknown"))
                ? currentUsername
                : currentUid;

        userDocRef = db.collection("users").document(docKey);

        userDocRef.get().addOnSuccessListener(snapshot -> {
            if (isAdded()) {
                if (snapshot.exists()) {
                    populateFields(snapshot);
                } else {
                    // Fallback: Query by UID field if document ID is not UID/Username
                    db.collection("users").whereEqualTo("uid", currentUid).get().addOnSuccessListener(querySnapshot -> {
                        if (isAdded() && !querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            userDocRef = doc.getReference();
                            populateFields(doc);
                        } else {
                            // If no document exists at all, ensure userDocRef uses currentUid for new record
                            userDocRef = db.collection("users").document(currentUid);
                            loadFromLocalPrefs();
                        }
                    });
                }
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                loadFromLocalPrefs();
            }
        });
    }

    private void loadFromLocalPrefs() {
        User localUser = prefManager.getUser();
        if (localUser != null) {
            if (etFullName != null) etFullName.setText(localUser.getFullName());
            if (etEmail != null) etEmail.setText(localUser.getEmail());
            if (etAddress != null) etAddress.setText(localUser.getAddress());
            if (etPhone != null) etPhone.setText(localUser.getPhone());
        }
    }

    private void populateFields(DocumentSnapshot snapshot) {
        // Retrieve and decrypt fields
        String encFullName = snapshot.getString("fullName");
        String encEmail = snapshot.getString("email");
        String encAddress = snapshot.getString("address");
        String encPhone = snapshot.getString("phone");

        if (etFullName != null) {
            etFullName.setText(FirebaseCryptoUtils.decrypt(encFullName, currentUid));
        }

        if (etEmail != null) {
            etEmail.setText(FirebaseCryptoUtils.decrypt(encEmail, currentUid));
        }

        if (etAddress != null) {
            etAddress.setText(FirebaseCryptoUtils.decrypt(encAddress, currentUid));
        }
        if (etPhone != null) {
            etPhone.setText(FirebaseCryptoUtils.decrypt(encPhone, currentUid));
        }
    }

    private void saveUserData() {
        if (currentUid == null || userDocRef == null) {
            Toast.makeText(getContext(), "Unable to save: Invalid session", Toast.LENGTH_SHORT).show();
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

        setFormEnabled(false);

        // Encrypt sensitive fields before Firestore update
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", FirebaseCryptoUtils.encrypt(name, currentUid));
        updates.put("email", FirebaseCryptoUtils.encrypt(email, currentUid));
        updates.put("address", FirebaseCryptoUtils.encrypt(address, currentUid));
        updates.put("phone", FirebaseCryptoUtils.encrypt(phone, currentUid));
        updates.put("uid", currentUid);

        // Use set with merge to ensure document exists
        userDocRef.set(updates, SetOptions.merge()).addOnCompleteListener(task -> {
            if (isAdded()) {
                setFormEnabled(true);

                if (task.isSuccessful()) {
                    // Update Local Preferences (Save plain text for local use)
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

    private void setFormEnabled(boolean enabled) {
        if (btnSave != null) {
            btnSave.setEnabled(enabled);
            btnSave.setText(enabled ? "Save Changes" : "Saving...");
        }
        if (etFullName != null) etFullName.setEnabled(enabled);
        if (etEmail != null) etEmail.setEnabled(enabled);
        if (etAddress != null) etAddress.setEnabled(enabled);
        if (etPhone != null) etPhone.setEnabled(enabled);
    }
}
