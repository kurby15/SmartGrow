package com.example.smartgrow.community;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.example.smartgrow.profile.User;
import com.example.smartgrow.utils.FirebaseCryptoUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

// Modern Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileFragment extends Fragment implements CommunityPostAdapter.OnPostInteractionListener {

    private String targetUid;
    private String currentUid;
    private TextView tvUsername, tvFullName, tvBio, tvNoPosts;
    private ImageView ivProfilePic;
    private View btnEditBio, btnSeeArchive;
    private RecyclerView rvPosts;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;

    // Firebase Auth & Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListenerRegistration, postsListenerRegistration;

    public static UserProfileFragment newInstance(String userUid) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("target_uid", userUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        if (getArguments() != null) {
            targetUid = getArguments().getString("target_uid");
        } else {
            targetUid = currentUid;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        tvUsername = view.findViewById(R.id.tv_username_display);
        tvFullName = view.findViewById(R.id.tv_full_name);
        tvBio = view.findViewById(R.id.tv_bio);
        tvNoPosts = view.findViewById(R.id.tv_no_posts);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnEditBio = view.findViewById(R.id.btn_edit_bio);
        btnSeeArchive = view.findViewById(R.id.btn_see_archive);
        rvPosts = view.findViewById(R.id.rv_user_posts);

        // Hide old social/followers layouts if present in XML
        View layoutFollowers = view.findViewById(R.id.layout_followers_click);
        View layoutFollowing = view.findViewById(R.id.layout_following_click);
        View btnFollow = view.findViewById(R.id.btn_follow);
        if (layoutFollowers != null) layoutFollowers.setVisibility(View.GONE);
        if (layoutFollowing != null) layoutFollowing.setVisibility(View.GONE);
        if (btnFollow != null) btnFollow.setVisibility(View.GONE);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (targetUid != null && targetUid.equals(currentUid)) {
            if (btnEditBio != null) {
                btnEditBio.setVisibility(View.VISIBLE);
                btnEditBio.setOnClickListener(v -> showModernEditBioSheet());
            }
            if (btnSeeArchive != null) {
                btnSeeArchive.setVisibility(View.VISIBLE);
                btnSeeArchive.setOnClickListener(v -> openArchive());
            }
            if (tvBio != null) {
                tvBio.setOnClickListener(v -> showModernEditBioSheet());
            }
        } else {
            if (btnEditBio != null) btnEditBio.setVisibility(View.GONE);
            if (btnSeeArchive != null) btnSeeArchive.setVisibility(View.GONE);
        }

        setupRecyclerView();
        fetchUserData();
        fetchUserPosts();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListenerRegistration != null) userListenerRegistration.remove();
        if (postsListenerRegistration != null) postsListenerRegistration.remove();
    }

    private void showModernEditBioSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_bio_sheet, null);
        sheet.setContentView(v);

        EditText etInput = v.findViewById(R.id.et_bio_input);
        String currentBio = tvBio.getText().toString();
        etInput.setText(currentBio.equals("No bio yet.") ? "" : currentBio);

        v.findViewById(R.id.btn_save_bio).setOnClickListener(view -> {
            String newBio = etInput.getText().toString().trim();
            db.collection("users").document(targetUid)
                    .update("bio", newBio)
                    .addOnSuccessListener(aVoid -> {
                        sheet.dismiss();
                        Toast.makeText(getContext(), "Bio updated! 🌿", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update bio: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        sheet.show();
    }

    private void openArchive() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ArchiveFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        adapter = new CommunityPostAdapter(postList, currentUid, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setAdapter(adapter);
    }

    private void fetchUserData() {
        // Query user doc by docId or UID field
        db.collection("users").whereEqualTo("uid", targetUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        populateUserProfile(doc);
                    } else {
                        // Fallback: Check if targetUid is used directly as document ID
                        db.collection("users").document(targetUid).get().addOnSuccessListener(doc -> {
                            if (isAdded() && doc.exists()) {
                                populateUserProfile(doc);
                            }
                        });
                    }
                });
    }

    private void populateUserProfile(DocumentSnapshot snapshot) {
        User user = snapshot.toObject(User.class);
        if (user != null) {
            String userUid = user.getUid() != null ? user.getUid() : targetUid;

            tvUsername.setText("@" + user.getUsername());

            // Decrypt Full Name
            String rawFullName = snapshot.getString("fullName");
            if (rawFullName != null && !rawFullName.isEmpty()) {
                try {
                    String decryptedName = FirebaseCryptoUtils.decrypt(rawFullName, userUid);
                    tvFullName.setText(decryptedName);
                } catch (Exception e) {
                    tvFullName.setText(rawFullName); // Fallback to raw string
                }
            } else {
                tvFullName.setText(user.getUsername());
            }

            String bioText = snapshot.getString("bio");
            if (bioText == null || bioText.isEmpty()) {
                tvBio.setText("No bio yet.");
                tvBio.setAlpha(0.5f);
            } else {
                tvBio.setText(bioText);
                tvBio.setAlpha(1.0f);
            }

            loadProfileImage(user.getProfilePic());
        }
    }

    private void loadProfileImage(String profileData) {
        if (profileData == null || profileData.isEmpty()) {
            ivProfilePic.setImageResource(R.drawable.ic_user);
            return;
        }
        if (profileData.startsWith("http")) {
            Glide.with(this).load(profileData).into(ivProfilePic);
        } else {
            try {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivProfilePic.setImageBitmap(bitmap);
            } catch (Exception e) {
                ivProfilePic.setImageResource(R.drawable.ic_user);
            }
        }
    }

    private void fetchUserPosts() {
        postsListenerRegistration = db.collection("posts")
                .whereEqualTo("userId", targetUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (!isAdded() || error != null || querySnapshot == null) return;

                    postList.clear();
                    for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                        CommunityPostModel post = snap.toObject(CommunityPostModel.class);
                        if (post != null) {
                            post.setPostId(snap.getId());
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvNoPosts.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onLikeClick(CommunityPostModel post) {
        if (currentUid == null) return;
        DocumentReference postRef = db.collection("posts").document(post.getPostId());
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);

        db.runTransaction(transaction -> {
            DocumentSnapshot likeSnap = transaction.get(likeRef);
            if (likeSnap.exists()) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likesCount", FieldValue.increment(-1));
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("timestamp", FieldValue.serverTimestamp());
                transaction.set(likeRef, likeData);
                transaction.update(postRef, "likesCount", FieldValue.increment(1));
            }
            return null;
        });
    }

    @Override
    public void onCommentClick(CommunityPostModel post) {
        Toast.makeText(getContext(), "Open comments from main forum", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserClick(String userUid) {
        if (!userUid.equals(targetUid)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, UserProfileFragment.newInstance(userUid))
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onMoreClick(View v, CommunityPostModel post) {
        if (getContext() == null) return;
        BottomSheetDialog optionsSheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_options_sheet, null);
        optionsSheet.setContentView(sheetView);

        LinearLayout layoutOwner = sheetView.findViewById(R.id.layout_owner_options);
        LinearLayout layoutOther = sheetView.findViewById(R.id.layout_other_user_options);

        if (post.getUserId() != null && post.getUserId().equals(currentUid)) {
            layoutOwner.setVisibility(View.VISIBLE);
            layoutOther.setVisibility(View.GONE);
        } else {
            layoutOwner.setVisibility(View.GONE);
            layoutOther.setVisibility(View.VISIBLE);
        }

        sheetView.findViewById(R.id.item_edit_post).setOnClickListener(view -> {
            optionsSheet.dismiss();
            showEditPostDialog(post);
        });

        sheetView.findViewById(R.id.item_archive_post).setOnClickListener(view -> {
            optionsSheet.dismiss();
            Toast.makeText(getContext(), "Post archived.", Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.item_move_trash).setOnClickListener(view -> {
            optionsSheet.dismiss();
            db.collection("posts").document(post.getPostId()).delete();
            Toast.makeText(getContext(), "Post moved to trash.", Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.item_hide_post).setOnClickListener(view -> {
            optionsSheet.dismiss();
            Toast.makeText(getContext(), "Post hidden.", Toast.LENGTH_SHORT).show();
        });

        optionsSheet.show();
    }

    private void showEditPostDialog(CommunityPostModel post) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_post, null);
        dialog.setContentView(v);

        EditText etContent = v.findViewById(R.id.et_edit_post_box);
        Button btnUpdate = v.findViewById(R.id.btn_update_post);
        MaterialCardView cardPreview = v.findViewById(R.id.card_edit_preview_container);
        ImageView ivPreview = v.findViewById(R.id.iv_edit_post_preview);
        ImageButton btnRemovePhoto = v.findViewById(R.id.btn_remove_edit_photo);

        etContent.setText(post.getContent());

        final String[] updatedImageBase64 = {post.getPostImageUri()};

        if (post.getPostImageUri() != null && !post.getPostImageUri().isEmpty()) {
            cardPreview.setVisibility(View.VISIBLE);
            if (post.getPostImageUri().startsWith("http")) {
                Glide.with(this).load(post.getPostImageUri()).into(ivPreview);
            } else if (post.getPostImageUri().length() > 1000) {
                byte[] decodedString = Base64.decode(post.getPostImageUri(), Base64.DEFAULT);
                ivPreview.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
            }
        }

        btnRemovePhoto.setOnClickListener(view -> {
            cardPreview.setVisibility(View.GONE);
            updatedImageBase64[0] = "";
        });

        btnUpdate.setOnClickListener(view -> {
            String newContent = etContent.getText().toString().trim();
            if (newContent.isEmpty() && (updatedImageBase64[0] == null || updatedImageBase64[0].isEmpty())) {
                Toast.makeText(getContext(), "Post cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            btnUpdate.setEnabled(false);
            btnUpdate.setText("Updating...");

            Map<String, Object> updates = new HashMap<>();
            updates.put("content", newContent);
            updates.put("postImageUri", updatedImageBase64[0]);

            db.collection("posts").document(post.getPostId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Post updated! ✨", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnUpdate.setEnabled(true);
                        btnUpdate.setText("Update Post");
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}