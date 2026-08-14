package com.example.smartgrow.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartgrow.R;
import com.example.smartgrow.auth.LoginActivity;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.settings.AccountSecurityFragment;
import com.example.smartgrow.settings.AppPreferencesFragment;
import com.example.smartgrow.settings.SupportInfoFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvRank;
    private Chip chipChoice2, chipChoice3, chipChoice4;
    private TextView tvPlantCount, tvScanCount, tvPostCount;
    private ImageView ivProfilePic;
    private View btnCameraBadge;
    private DatabaseReference userRef;
    private String currentUsername;
    private SharedPrefManager prefManager;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();

        tvFullName = view.findViewById(R.id.tv_user_display_name);
        tvRank = view.findViewById(R.id.tv_user_rank);
        
        // 🌿 Mapping the new Chips
        chipChoice2 = view.findViewById(R.id.tv_choice2);
        chipChoice3 = view.findViewById(R.id.tv_choice3);
        chipChoice4 = view.findViewById(R.id.tv_choice4);

        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnCameraBadge = view.findViewById(R.id.btn_camera_badge);

        tvPlantCount = view.findViewById(R.id.tv_stats_plants);
        tvScanCount = view.findViewById(R.id.tv_stats_scans);
        tvPostCount = view.findViewById(R.id.tv_stats_posts);

        setupNavigation(view);

        if (!currentUsername.isEmpty() && !currentUsername.equals("unknown")) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
            fetchUserData();
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> showCustomLogoutDialog());
        if (btnCameraBadge != null) btnCameraBadge.setOnClickListener(v -> showImageSourceOptions());
        if (ivProfilePic != null) ivProfilePic.setOnClickListener(v -> showImageSourceOptions());

        return view;
    }

    private void setupNavigation(View view) {
        view.findViewById(R.id.btn_preferences).setOnClickListener(v -> navigateTo(new AppPreferencesFragment()));
        view.findViewById(R.id.btn_security).setOnClickListener(v -> navigateTo(new AccountSecurityFragment()));
        view.findViewById(R.id.btn_support).setOnClickListener(v -> navigateTo(new SupportInfoFragment()));
    }

    private void navigateTo(Fragment fragment) {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void fetchUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (tvFullName != null) tvFullName.setText(user.getFullName());
                    if (tvRank != null) tvRank.setText(user.getChoice1() != null ? user.getChoice1() : "Beginner Grower");

                    // 🏷️ Dynamic Interest Tags Logic
                    updateInterestTag(chipChoice2, user.getChoice2());
                    updateInterestTag(chipChoice3, user.getChoice3());
                    updateInterestTag(chipChoice4, user.getChoice4());

                    loadProfileImage(user.getProfilePic());
                    loadUserStats();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateInterestTag(Chip chip, String value) {
        if (chip == null) return;
        if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("None")) {
            chip.setText(value);
            chip.setVisibility(View.VISIBLE);
        } else {
            chip.setVisibility(View.GONE);
        }
    }

    private void loadProfileImage(String profileData) {
        if (profileData != null && !profileData.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (ivProfilePic != null) ivProfilePic.setImageBitmap(decodedByte);
            } catch (Exception e) {
                if (ivProfilePic != null) ivProfilePic.setImageResource(R.drawable.ic_user);
            }
        }
    }

    private void loadUserStats() {
        // Plants Count
        userRef.child("plants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                long count = snapshot.getChildrenCount();
                tvPlantCount.setText(count + (count == 1 ? " Plant" : " Plants"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Posts Count
        FirebaseDatabase.getInstance().getReference("posts").orderByChild("userId").equalTo(currentUsername)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        long count = snapshot.getChildrenCount();
                        tvPostCount.setText(count + (count == 1 ? " Post" : " Posts"));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(result.getData().getData());
                        uploadProfilePic(BitmapFactory.decodeStream(is));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        if (photo != null) uploadProfilePic(photo);
                    }
                }
            });

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(requireContext()).setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    else if (which == 1) galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                }).show();
    }

    private void uploadProfilePic(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        userRef.child("profilePic").setValue(base64Image).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ivProfilePic.setImageBitmap(resized);
                prefManager.saveProfilePic(base64Image);
                Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCustomLogoutDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialogView.findViewById(R.id.btn_logout_yes).setOnClickListener(v -> {
            dialog.dismiss();
            prefManager.logout(requireContext());
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
        dialogView.findViewById(R.id.btn_logout_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
