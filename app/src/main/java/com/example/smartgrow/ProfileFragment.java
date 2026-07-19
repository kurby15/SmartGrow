package com.example.smartgrow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvRank;
    private TextView tvChoice2, tvChoice3, tvChoice4;
    private TextView tvPlantCount, tvScanCount, tvPostCount;
    private ImageView ivProfilePic;
    private View btnCameraBadge;
    private DatabaseReference databaseReference;
    private String currentUsername;
    private SharedPreferences preferences;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind views
        tvFullName = view.findViewById(R.id.tv_user_display_name);
        tvRank = view.findViewById(R.id.tv_user_rank);
        tvChoice2 = view.findViewById(R.id.tv_choice2);
        tvChoice3 = view.findViewById(R.id.tv_choice3);
        tvChoice4 = view.findViewById(R.id.tv_choice4);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnCameraBadge = view.findViewById(R.id.btn_camera_badge);
        
        // Stats
        tvPlantCount = view.findViewById(R.id.tv_stats_plants);
        tvScanCount = view.findViewById(R.id.tv_stats_scans);
        tvPostCount = view.findViewById(R.id.tv_stats_posts);

        // Click listeners for navigation
        view.findViewById(R.id.btn_preferences).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AppPreferencesFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.btn_security).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AccountSecurityFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.btn_support).setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SupportInfoFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Initialize Shared Preferences safely
        if (getActivity() != null) {
            preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
            currentUsername = preferences.getString("current_username", "");

            if (!currentUsername.isEmpty()) {
                databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername);
                fetchUserData();
            }

            // Logout Listener
            view.findViewById(R.id.btn_logout).setOnClickListener(v -> showCustomLogoutDialog());
        }

        // Profile Picture Upload Listeners
        if (btnCameraBadge != null) btnCameraBadge.setOnClickListener(v -> showImageSourceOptions());
        if (ivProfilePic != null) ivProfilePic.setOnClickListener(v -> showImageSourceOptions());

        return view;
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        uploadProfilePic(bitmap);
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
                        if (photo != null) {
                            uploadProfilePic(photo);
                        }
                    }
                }
            });

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void uploadProfilePic(Bitmap bitmap) {
        // 🚀 Resize for efficiency
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        if (databaseReference != null) {
            databaseReference.child("profilePic").setValue(base64Image).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ivProfilePic.setImageBitmap(resized);
                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to update profile picture.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showCustomLogoutDialog() {
        if (getContext() == null || getActivity() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        Button btnYes = dialogView.findViewById(R.id.btn_logout_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_logout_no);
        if (btnYes != null) {
            btnYes.setOnClickListener(v -> {
                dialog.dismiss();
                performLogout();
            });
        }
        if (btnNo != null) btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void performLogout() {
        if (preferences != null && getActivity() != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void fetchUserData() {
        if (databaseReference == null) return;

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String fullName = user.getFullName();
                        if (tvFullName != null) tvFullName.setText(fullName);

                        if (tvRank != null && user.getChoice1() != null) {
                            tvRank.setText(user.getChoice1());
                        }

                        if (tvChoice2 != null && user.getChoice2() != null) tvChoice2.setText(user.getChoice2());
                        if (tvChoice3 != null && user.getChoice3() != null) tvChoice3.setText(user.getChoice3());
                        if (tvChoice4 != null && user.getChoice4() != null) tvChoice4.setText(user.getChoice4());

                        // 🖼️ Load Profile Pic
                        if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(user.getProfilePic(), Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (ivProfilePic != null) ivProfilePic.setImageBitmap(decodedByte);
                            } catch (Exception e) {
                                if (ivProfilePic != null) ivProfilePic.setImageResource(R.drawable.ic_user);
                            }
                        }

                        // 📈 Load Real Stats
                        loadUserStats();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserStats() {
        if (databaseReference == null) return;

        // 1. Count Plants
        databaseReference.child("plants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                long count = snapshot.getChildrenCount();
                if (tvPlantCount != null) tvPlantCount.setText(count + (count == 1 ? " Plant" : " Plants"));

                // 2. Count Scans (Sum of logs across all plants)
                int totalScans = 0;
                for (DataSnapshot plantSnapshot : snapshot.getChildren()) {
                    if (plantSnapshot.hasChild("logs")) {
                        totalScans += plantSnapshot.child("logs").getChildrenCount();
                    }
                }
                if (tvScanCount != null) tvScanCount.setText(totalScans + (totalScans == 1 ? " Scan" : " Scans"));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Count Posts (From community posts node where username matches)
        FirebaseDatabase.getInstance().getReference("posts").orderByChild("username").equalTo(currentUsername)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        long count = snapshot.getChildrenCount();
                        if (tvPostCount != null) tvPostCount.setText(count + (count == 1 ? " Post" : " Posts"));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}