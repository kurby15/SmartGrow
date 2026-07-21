package com.example.smartgrow;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private EditText etFullName, etEmail, etAddress, etPhone;
    private MaterialButton btnSave;
    private DatabaseReference userRef;
    private String currentUsername;
    private SharedPrefManager prefManager;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();
        if (currentUsername != null && !currentUsername.equals("unknown")) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        etFullName = view.findViewById(R.id.et_fullname);
        etEmail = view.findViewById(R.id.et_email);
        etAddress = view.findViewById(R.id.et_address);
        etPhone = view.findViewById(R.id.et_phone);
        btnSave = view.findViewById(R.id.btn_save_edit_info);

        view.findViewById(R.id.btn_back_edit_info).setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (userRef != null) {
            fetchUserData();
        }

        btnSave.setOnClickListener(v -> saveUserData());

        return view;
    }

    private void fetchUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (etFullName != null) etFullName.setText(user.getFullName());
                        if (etEmail != null) etEmail.setText(user.getEmail());
                        // Kung may fields na address at phone sa User model
                        // Dito natin ilalagay kung meron na sa Firebase
                        if (snapshot.hasChild("address")) etAddress.setText(snapshot.child("address").getValue(String.class));
                        if (snapshot.hasChild("phone")) etPhone.setText(snapshot.child("phone").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveUserData() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("email", email);
        updates.put("address", address);
        updates.put("phone", phone);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (isAdded()) {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                if (task.isSuccessful()) {
                    // 🔄 Sync to SharedPrefManager
                    User updatedUser = new User();
                    updatedUser.setUsername(currentUsername);
                    updatedUser.setFullName(name);
                    updatedUser.setEmail(email);
                    prefManager.saveUser(updatedUser);

                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
