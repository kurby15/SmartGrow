package com.example.smartgrow.community;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {

    private ShapeableImageView imgUserAvatar;
    private TextView tvUsername, tvLocationTag;
    private EditText etPostBox;
    private LinearLayout btnAddPhoto, layoutLocation;
    private MaterialButton btnSubmitPost;
    private MaterialCardView cardPreview;
    private ImageView ivPostPreview;
    private ImageButton btnRemovePhoto;

    private DatabaseReference postsRef;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;

    private Uri selectedImageUri = null;
    private String currentUserId, currentUserFullName, currentUserProfilePic;
    private String detectedLocation = "";

    private ActivityResultLauncher<String> imagePickerLauncher;
    private SharedPrefManager prefManager;

    public static CreatePostBottomSheet newInstance() {
        return new CreatePostBottomSheet();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        storageRef = FirebaseStorage.getInstance().getReference("post_images");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUserId = prefManager.getUsername();
        currentUserFullName = prefManager.getFullName();
        currentUserProfilePic = prefManager.getProfilePic();

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                updateImagePreview();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imgUserAvatar = view.findViewById(R.id.img_create_post);
        tvUsername = view.findViewById(R.id.tv_identity_user_name);
        layoutLocation = view.findViewById(R.id.layout_location_tag);
        etPostBox = view.findViewById(R.id.et_create_post_box);
        btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        btnSubmitPost = view.findViewById(R.id.btn_submit_post);
        cardPreview = view.findViewById(R.id.card_preview_container);
        ivPostPreview = view.findViewById(R.id.iv_post_preview);
        btnRemovePhoto = view.findViewById(R.id.btn_remove_photo);

        tvUsername.setText(currentUserFullName);
        loadProfileImage(currentUserProfilePic, imgUserAvatar);

        btnAddPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> {
            selectedImageUri = null;
            updateImagePreview();
        });

        btnSubmitPost.setOnClickListener(v -> {
            String content = etPostBox.getText().toString().trim();
            if (content.isEmpty() && selectedImageUri == null) return;

            if (ProfanityFilter.hasProfanity(content)) {
                Toast.makeText(getContext(), "Prohibited words detected.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitPost.setEnabled(false);
            btnSubmitPost.setText("Posting...");
            if (selectedImageUri != null) uploadImageAndPost(content);
            else savePostToDatabase(content, null);
        });
    }

    private void uploadImageAndPost(String content) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();

            String fileName = "post_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageRef.child(fileName);

            fileRef.putBytes(data).continueWithTask(task -> {
                if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                return fileRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    savePostToDatabase(content, task.getResult().toString());
                } else {
                    String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(getContext(), "Upload Failed: " + err, Toast.LENGTH_LONG).show();
                    resetButton();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Image Error", Toast.LENGTH_SHORT).show();
            resetButton();
        }
    }

    private void resetButton() {
        btnSubmitPost.setEnabled(true);
        btnSubmitPost.setText("Post");
    }

    private void savePostToDatabase(String content, String imageUrl) {
        String postId = postsRef.push().getKey();
        CommunityPostModel post = new CommunityPostModel(postId, currentUserFullName, currentUserId, currentUserProfilePic, System.currentTimeMillis(), content, imageUrl, detectedLocation);
        if (postId != null) {
            postsRef.child(postId).setValue(post).addOnSuccessListener(aVoid -> {
                dismiss();
                selectedImageUri = null;
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            });
        }
    }

    private void loadProfileImage(String profileData, ImageView imageView) {
        if (profileData == null || profileData.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_user);
            return;
        }
        try {
            if (profileData.length() > 500) {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Glide.with(this).load(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length)).circleCrop().into(imageView);
            } else {
                Glide.with(this).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_user);
        }
    }

    private void updateImagePreview() {
        if (selectedImageUri != null) {
            ivPostPreview.setImageURI(selectedImageUri);
            cardPreview.setVisibility(View.VISIBLE);
        } else {
            cardPreview.setVisibility(View.GONE);
        }
    }
}
