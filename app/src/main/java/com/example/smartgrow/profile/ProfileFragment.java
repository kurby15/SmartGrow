package com.example.smartgrow.profile;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.example.smartgrow.auth.LoginActivity;
import com.example.smartgrow.core.SharedPrefManager;
import com.example.smartgrow.settings.AccountSecurityFragment;
import com.example.smartgrow.settings.AppPreferencesFragment;
import com.example.smartgrow.settings.SupportInfoFragment;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.chip.Chip;

// Modern Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvRank;
    private Chip chipChoice2, chipChoice3, chipChoice4;
    private TextView tvPlantCount, tvScanCount, tvPostCount;
    private ImageView ivProfilePic;
    private View btnCameraBadge;

    // Firebase Auth & Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;
    private ListenerRegistration userListenerRegistration;

    private String currentUid;
    private String currentUsername;
    private SharedPrefManager prefManager;

    private Uri cameraImageUri;

    public ProfileFragment() {
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

        // Restore camera URI on config change
        if (savedInstanceState != null) {
            cameraImageUri = savedInstanceState.getParcelable("cameraImageUri");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putParcelable("cameraImageUri", cameraImageUri);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvFullName = view.findViewById(R.id.tv_user_display_name);
        tvRank = view.findViewById(R.id.tv_user_rank);

        // Dynamic Interest Chips
        chipChoice2 = view.findViewById(R.id.tv_choice2);
        chipChoice3 = view.findViewById(R.id.tv_choice3);
        chipChoice4 = view.findViewById(R.id.tv_choice4);

        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnCameraBadge = view.findViewById(R.id.btn_camera_badge);

        tvPlantCount = view.findViewById(R.id.tv_stats_plants);
        tvScanCount = view.findViewById(R.id.tv_stats_scans);
        tvPostCount = view.findViewById(R.id.tv_stats_posts);

        setupNavigation(view);

        if (currentUid != null) {
            fetchUserData();
        } else {
            Toast.makeText(getContext(), "User session not found.", Toast.LENGTH_SHORT).show();
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> showCustomLogoutDialog());
        if (btnCameraBadge != null) btnCameraBadge.setOnClickListener(v -> showImageSourceOptions());
        if (ivProfilePic != null) ivProfilePic.setOnClickListener(v -> showImageSourceOptions());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
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
        String docKey = (currentUsername != null && !currentUsername.trim().isEmpty() && !currentUsername.equals("unknown"))
                ? currentUsername
                : currentUid;

        userDocRef = db.collection("users").document(docKey);

        userListenerRegistration = userDocRef.addSnapshotListener((snapshot, error) -> {
            if (!isAdded() || error != null) return;

            if (snapshot != null && snapshot.exists()) {
                populateUserData(snapshot);
            } else {
                // Dual document-key fallback check using UID field query
                db.collection("users").whereEqualTo("uid", currentUid).limit(1).get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (isAdded() && !querySnapshot.isEmpty()) {
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                userDocRef = doc.getReference();
                                populateUserData(doc);
                            }
                        });
            }
        });

        loadUserStats();
    }

    private void populateUserData(DocumentSnapshot snapshot) {
        String rawFullName = snapshot.getString("fullName");
        String choice1 = snapshot.getString("choice1");
        String choice2 = snapshot.getString("choice2");
        String choice3 = snapshot.getString("choice3");
        String choice4 = snapshot.getString("choice4");
        String profilePic = snapshot.getString("profilePic");

        // Decrypt display name
        if (tvFullName != null && rawFullName != null) {
            String decryptedName = FirebaseCryptoUtils.decrypt(rawFullName, currentUid);
            tvFullName.setText(decryptedName);
        }

        if (tvRank != null) {
            tvRank.setText(choice1 != null && !choice1.isEmpty() ? choice1 : "Beginner Grower");
        }

        // Interest Tags
        updateInterestTag(chipChoice2, choice2);
        updateInterestTag(chipChoice3, choice3);
        updateInterestTag(chipChoice4, choice4);

        loadProfileImage(profilePic);
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
        if (ivProfilePic == null) return;

        if (profileData == null || profileData.isEmpty()) {
            ivProfilePic.setImageResource(R.drawable.ic_user);
            return;
        }

        try {
            if (profileData.startsWith("http")) {
                Glide.with(this).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(ivProfilePic);
            } else if (profileData.length() > 500) {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                Glide.with(this).load(bitmap).circleCrop().into(ivProfilePic);
            } else {
                Glide.with(this).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(ivProfilePic);
            }
        } catch (Exception e) {
            ivProfilePic.setImageResource(R.drawable.ic_user);
        }
    }

    private void loadUserStats() {
        if (userDocRef == null) return;

        // Count user plants subcollection
        userDocRef.collection("plants").addSnapshotListener((querySnapshot, error) -> {
            if (!isAdded() || error != null || querySnapshot == null) return;
            int count = querySnapshot.size();
            if (tvPlantCount != null) {
                tvPlantCount.setText(count + (count == 1 ? " Plant" : " Plants"));
            }
        });

        // Count posts created by current user
        db.collection("posts")
                .whereEqualTo("userId", currentUid)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (!isAdded() || error != null || querySnapshot == null) return;
                    int count = querySnapshot.size();
                    if (tvPostCount != null) {
                        tvPostCount.setText(count + (count == 1 ? " Post" : " Posts"));
                    }
                });
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCameraIntent();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(result.getData().getData());
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) uploadProfilePic(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load gallery image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        // Priority 1: Read full quality image from URI
                        if (cameraImageUri != null) {
                            InputStream is = requireContext().getContentResolver().openInputStream(cameraImageUri);
                            Bitmap photo = BitmapFactory.decodeStream(is);
                            if (photo != null) {
                                uploadProfilePic(photo);
                                return;
                            }
                        }

                        // Priority 2: Fallback to thumbnail from intent extras if URI read failed
                        if (result.getData() != null && result.getData().getExtras() != null) {
                            Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                            if (photo != null) {
                                uploadProfilePic(photo);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to process camera image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(requireContext()).setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    }
                }).show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile
                );
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraLauncher.launch(cameraIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback launch directly without custom FileProvider if error occurs
            cameraLauncher.launch(cameraIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void uploadProfilePic(Bitmap bitmap) {
        if (userDocRef == null) return;

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        userDocRef.update("profilePic", base64Image).addOnCompleteListener(task -> {
            if (isAdded() && task.isSuccessful()) {
                prefManager.saveProfilePic(base64Image);
                loadProfileImage(base64Image);
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
            if (mAuth != null) mAuth.signOut();
            prefManager.logout(requireContext());
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        dialogView.findViewById(R.id.btn_logout_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
