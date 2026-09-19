package com.example.smartgrow.profile;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;

    private ListenerRegistration userListenerRegistration;
    private ListenerRegistration plantListenerRegistration;
    private ListenerRegistration postListenerRegistration;

    private String currentUid;
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

        Context context = getContext();
        if (context != null) {
            prefManager = SharedPrefManager.getInstance(context);
        }

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

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
        removeListeners();
    }

    private void removeListeners() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
        if (plantListenerRegistration != null) {
            plantListenerRegistration.remove();
            plantListenerRegistration = null;
        }
        if (postListenerRegistration != null) {
            postListenerRegistration.remove();
            postListenerRegistration = null;
        }
    }

    private void setupNavigation(View view) {
        view.findViewById(R.id.btn_preferences).setOnClickListener(v -> navigateTo(new AppPreferencesFragment()));
        view.findViewById(R.id.btn_account).setOnClickListener(v -> navigateTo(new AccountSecurityFragment()));
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
        if (currentUid == null || currentUid.trim().isEmpty()) {
            Toast.makeText(getContext(), "User session invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Always query directly by document key (UID)
        userDocRef = db.collection("users").document(currentUid);

        userListenerRegistration = userDocRef.addSnapshotListener((snapshot, error) -> {
            if (!isAdded() || error != null) return;

            if (snapshot != null && snapshot.exists()) {
                populateUserData(snapshot);
            } else {
                Toast.makeText(getContext(), "User profile not found in database.", Toast.LENGTH_SHORT).show();
            }
        });

        loadUserStats();
    }

    private void populateUserData(DocumentSnapshot snapshot) {
        if (!isAdded()) return;

        String rawFullName = snapshot.getString("fullName");
        String choice1 = snapshot.getString("choice1");
        String choice2 = snapshot.getString("choice2");
        String choice3 = snapshot.getString("choice3");
        String choice4 = snapshot.getString("choice4");
        String profilePic = snapshot.getString("profilePic");

        if (tvFullName != null && rawFullName != null) {
            String decryptedName = FirebaseCryptoUtils.decrypt(rawFullName, currentUid);
            tvFullName.setText(decryptedName);
        }

        if (tvRank != null) {
            tvRank.setText(choice1 != null && !choice1.isEmpty() ? choice1 : "Beginner Grower");
        }

        updateInterestTag(chipChoice2, choice2);
        updateInterestTag(chipChoice3, choice3);
        updateInterestTag(chipChoice4, choice4);

        loadProfileImage(profilePic);
    }

    private void updateInterestTag(Chip chip, String value) {
        if (chip == null) return;
        if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("None")) {
            chip.setText(value);

            try {
                android.graphics.Typeface typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_bold);
                if (typeface != null) {
                    chip.setTypeface(typeface);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            chip.setVisibility(View.VISIBLE);
        } else {
            chip.setVisibility(View.GONE);
        }
    }

    private void loadProfileImage(String profileData) {
        if (ivProfilePic == null || !isAdded()) return;

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

        plantListenerRegistration = userDocRef.collection("plants").addSnapshotListener((querySnapshot, error) -> {
            if (!isAdded() || error != null || querySnapshot == null) return;
            int count = querySnapshot.size();
            if (tvPlantCount != null) {
                tvPlantCount.setText(count + (count == 1 ? " Plant" : " Plants"));
            }
        });

        postListenerRegistration = db.collection("posts")
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
                } else if (isAdded()) {
                    Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null && isAdded()) {
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
                if (result.getResultCode() == RESULT_OK && isAdded()) {
                    try {
                        if (cameraImageUri != null) {
                            InputStream is = requireContext().getContentResolver().openInputStream(cameraImageUri);
                            Bitmap photo = BitmapFactory.decodeStream(is);
                            if (photo != null) {
                                uploadProfilePic(photo);
                                return;
                            }
                        }

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
        if (!isAdded()) return;
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
        if (!isAdded()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraIntent() {
        if (!isAdded()) return;
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
            cameraLauncher.launch(cameraIntent);
        }
    }

    private File createImageFile() throws IOException {
        if (!isAdded()) return null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void uploadProfilePic(Bitmap bitmap) {
        if (userDocRef == null || !isAdded()) return;

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        userDocRef.update("profilePic", base64Image).addOnCompleteListener(task -> {
            if (isAdded() && task.isSuccessful()) {
                if (prefManager != null) {
                    prefManager.saveProfilePic(base64Image);
                }
                loadProfileImage(base64Image);
                Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCustomLogoutDialog() {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btn_logout_yes).setOnClickListener(v -> {
            dialog.dismiss();

            removeListeners();

            if (mAuth != null) {
                mAuth.signOut();
            }

            Context ctx = getContext();
            if (ctx != null) {
                SharedPrefManager.getInstance(ctx).logout(ctx);
            }

            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        dialogView.findViewById(R.id.btn_logout_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}