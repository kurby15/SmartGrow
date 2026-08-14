package com.example.smartgrow.community;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommunityForumFragment extends Fragment implements CommunityPostAdapter.OnPostInteractionListener {

    private RecyclerView recyclerView;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private MaterialCardView cardMind;
    private ImageView imgUserAvatar, btnMyProfile;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase Services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration postsListenerRegistration;
    private FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri = null;
    private String currentUid, currentUsername, currentUserFullName, currentUserProfilePic;
    private String detectedLocation = "";
    private String savedDraftContent = "";

    public CommunityForumFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        refreshUserInfo();

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                showCreatePostDialog();
            }
        });
    }

    private void refreshUserInfo() {
        if (getContext() == null) return;
        SharedPrefManager prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();
        currentUserFullName = prefManager.getFullName();
        currentUserProfilePic = prefManager.getProfilePic();
    }

    private String getUserDocId() {
        return (currentUsername != null && !currentUsername.isEmpty()) ? currentUsername : currentUid;
    }

    private void loadProfileImage(String profileData, ImageView imageView) {
        if (profileData == null || profileData.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_user);
            return;
        }
        try {
            if (profileData.startsWith("http")) {
                Glide.with(this).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(imageView);
            } else if (profileData.length() > 500) {
                byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
                Glide.with(this).load(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length)).circleCrop().into(imageView);
            } else {
                Glide.with(this).load(profileData).placeholder(R.drawable.ic_user).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_user);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_forum, container, false);

        cardMind = view.findViewById(R.id.card_mind);
        imgUserAvatar = view.findViewById(R.id.img_user);
        btnMyProfile = view.findViewById(R.id.btn_my_profile);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_forum);

        if (imgUserAvatar != null) loadProfileImage(currentUserProfilePic, imgUserAvatar);
        if (btnMyProfile != null) {
            loadProfileImage(currentUserProfilePic, btnMyProfile);
            btnMyProfile.setOnClickListener(v -> onUserClick(currentUid));
        }

        if (cardMind != null) {
            cardMind.setOnClickListener(v -> {
                selectedImageUri = null;
                savedDraftContent = "";
                showCreatePostDialog();
            });
        }

        if (imgUserAvatar != null) {
            imgUserAvatar.setOnClickListener(v -> onUserClick(currentUid));
        }

        View btnCamera = view.findViewById(R.id.btn_camera_icon);
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                selectedImageUri = null;
                savedDraftContent = "";
                imagePickerLauncher.launch("image/*");
            });
        }

        recyclerView = view.findViewById(R.id.rv_forum_feed);
        postList = new ArrayList<>();
        adapter = new CommunityPostAdapter(postList, currentUid, this);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::listenForPosts);
            swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#0C6211"));
        }

        listenForPosts();
        fetchLocationForTracking();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsListenerRegistration != null) {
            postsListenerRegistration.remove();
        }
    }

    private void fetchLocationForTracking() {
        if (!isAdded() || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null && isAdded()) {
                try {
                    List<android.location.Address> addresses = new android.location.Geocoder(requireContext()).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        detectedLocation = (addresses.get(0).getLocality() != null ? addresses.get(0).getLocality() : addresses.get(0).getAdminArea());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void listenForPosts() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        if (currentUid == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userDocId = getUserDocId();

        // Retrieve hidden posts for the active user
        db.collection("users").document(userDocId).collection("hiddenPosts")
                .get()
                .addOnCompleteListener(task -> {
                    Set<String> hiddenIds = new HashSet<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            hiddenIds.add(doc.getId());
                        }
                    }

                    if (postsListenerRegistration != null) postsListenerRegistration.remove();

                    postsListenerRegistration = db.collection("posts")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((querySnapshot, error) -> {
                                if (!isAdded() || error != null || querySnapshot == null) {
                                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                    return;
                                }

                                postList.clear();
                                for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                                    CommunityPostModel post = snap.toObject(CommunityPostModel.class);
                                    if (post != null) {
                                        post.setPostId(snap.getId());
                                        if (!post.isArchived() && !hiddenIds.contains(post.getPostId())) {
                                            postList.add(post);
                                        }
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            });
                });
    }

    private void showCreatePostDialog() {
        refreshUserInfo();
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_post, null);
        dialog.setContentView(v);

        EditText etContent = v.findViewById(R.id.et_create_post_box);
        Button btnPost = v.findViewById(R.id.btn_submit_post);
        ImageView imgAvatar = v.findViewById(R.id.img_create_post);
        TextView tvName = v.findViewById(R.id.tv_identity_user_name);
        MaterialCardView cardPreview = v.findViewById(R.id.card_preview_container);
        ImageView ivPostPreview = v.findViewById(R.id.iv_post_preview);

        if (tvName != null) tvName.setText(currentUsername);
        if (imgAvatar != null) loadProfileImage(currentUserProfilePic, imgAvatar);
        if (etContent != null) etContent.setText(savedDraftContent);
        if (selectedImageUri != null) {
            if (ivPostPreview != null) ivPostPreview.setImageURI(selectedImageUri);
            if (cardPreview != null) cardPreview.setVisibility(View.VISIBLE);
        }

        View btnAddPhoto = v.findViewById(R.id.btn_add_photo);
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(view -> {
                savedDraftContent = etContent != null ? etContent.getText().toString() : "";
                imagePickerLauncher.launch("image/*");
                dialog.dismiss();
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(view -> {
                String content = etContent != null ? etContent.getText().toString().trim() : "";
                if (content.isEmpty() && selectedImageUri == null) return;
                if (ProfanityFilter.hasProfanity(content)) {
                    Toast.makeText(getContext(), "Prohibited words detected.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnPost.setEnabled(false);
                btnPost.setText("Posting...");
                processAndPost(content, dialog);
            });
        }

        dialog.show();
    }

    private void processAndPost(String content, BottomSheetDialog dialog) {
        String base64Image = "";
        if (selectedImageUri != null) {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 600, (int)(600 * ((double)bitmap.getHeight()/bitmap.getWidth())), true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Image error, posting without photo.", Toast.LENGTH_SHORT).show();
            }
        }
        savePostToDatabase(content, base64Image, dialog);
    }

    private void savePostToDatabase(String content, String imageBase64, BottomSheetDialog dialog) {
        DocumentReference newPostRef = db.collection("posts").document();

        CommunityPostModel post = new CommunityPostModel(
                newPostRef.getId(),
                currentUid,
                currentUsername,
                currentUserProfilePic,
                System.currentTimeMillis(),
                content,
                imageBase64,
                detectedLocation
        );

        newPostRef.set(post)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    showSuccessDialog();
                    selectedImageUri = null;
                    savedDraftContent = "";
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Post failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Button btn = dialog.findViewById(R.id.btn_submit_post);
                    if (btn != null) {
                        btn.setEnabled(true);
                        btn.setText("Post");
                    }
                });
    }

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_post_success);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View btnSuccess = dialog.findViewById(R.id.btn_success);
        if (btnSuccess != null) {
            btnSuccess.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
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
        showCommentsDialog(post);
    }

    @Override
    public void onUserClick(String userUid) {
        if (isAdded()) {
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
            if (layoutOwner != null) layoutOwner.setVisibility(View.VISIBLE);
            if (layoutOther != null) layoutOther.setVisibility(View.GONE);
        } else {
            if (layoutOwner != null) layoutOwner.setVisibility(View.GONE);
            if (layoutOther != null) layoutOther.setVisibility(View.VISIBLE);
        }

        View itemEdit = sheetView.findViewById(R.id.item_edit_post);
        if (itemEdit != null) {
            itemEdit.setOnClickListener(view -> {
                optionsSheet.dismiss();
                showEditPostDialog(post);
            });
        }

        View itemArchive = sheetView.findViewById(R.id.item_archive_post);
        if (itemArchive != null) {
            itemArchive.setOnClickListener(view -> {
                optionsSheet.dismiss();
                db.collection("posts").document(post.getPostId())
                        .update("archived", true)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Post moved to Archive.", Toast.LENGTH_SHORT).show());
            });
        }

        View itemMoveTrash = sheetView.findViewById(R.id.item_move_trash);
        if (itemMoveTrash != null) {
            itemMoveTrash.setOnClickListener(view -> {
                optionsSheet.dismiss();
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Move to Trash?")
                        .setMessage("This post will be permanently deleted.")
                        .setPositiveButton("Move", (d, w) -> {
                            db.collection("posts").document(post.getPostId()).delete();
                            Toast.makeText(getContext(), "Post moved to trash.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        View itemHidePost = sheetView.findViewById(R.id.item_hide_post);
        if (itemHidePost != null) {
            itemHidePost.setOnClickListener(view -> {
                optionsSheet.dismiss();
                Map<String, Object> hideMap = new HashMap<>();
                hideMap.put("hiddenAt", FieldValue.serverTimestamp());

                String userDocId = getUserDocId();

                db.collection("users").document(userDocId)
                        .collection("hiddenPosts").document(post.getPostId())
                        .set(hideMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Post hidden. You won't see this again.", Toast.LENGTH_SHORT).show();
                            listenForPosts();
                        });
            });
        }

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

        if (etContent != null) etContent.setText(post.getContent());

        final String[] updatedImageBase64 = {post.getPostImageUri()};

        if (post.getPostImageUri() != null && !post.getPostImageUri().isEmpty()) {
            if (cardPreview != null) cardPreview.setVisibility(View.VISIBLE);
            if (ivPreview != null) {
                if (post.getPostImageUri().startsWith("http")) {
                    Glide.with(this).load(post.getPostImageUri()).into(ivPreview);
                } else if (post.getPostImageUri().length() > 1000) {
                    byte[] decodedString = Base64.decode(post.getPostImageUri(), Base64.DEFAULT);
                    ivPreview.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                }
            }
        }

        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(view -> {
                if (cardPreview != null) cardPreview.setVisibility(View.GONE);
                updatedImageBase64[0] = "";
            });
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(view -> {
                String newContent = etContent != null ? etContent.getText().toString().trim() : "";
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
        }

        dialog.show();
    }

    private void showCommentsDialog(CommunityPostModel post) {
        refreshUserInfo();
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_comments, null);
        dialog.setContentView(v);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                lp.height = (int) (screenHeight * 0.9);
                bottomSheet.setLayoutParams(lp);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        ProgressBar pb = v.findViewById(R.id.pb_comments_loading);
        TextView tvNoComments = v.findViewById(R.id.tv_no_comments);
        RecyclerView rvComments = v.findViewById(R.id.rv_comments);
        EditText etComment = v.findViewById(R.id.et_comment_input);
        ImageButton btnSend = v.findViewById(R.id.btn_send_comment);
        ImageView imgAvatar = v.findViewById(R.id.iv_comment_input_avatar);

        if (pb != null) pb.setVisibility(View.VISIBLE);
        if (imgAvatar != null) loadProfileImage(currentUserProfilePic, imgAvatar);

        List<CommentModel> commentList = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(commentList);
        commentAdapter.setOnUserClickListener(this::onUserClick);
        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
            rvComments.setAdapter(commentAdapter);
        }

        // Realtime listener for post comments subcollection
        db.collection("posts").document(post.getPostId()).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (error != null || querySnapshot == null) return;

                    commentList.clear();
                    for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                        CommentModel c = snap.toObject(CommentModel.class);
                        if (c != null) {
                            c.setCommentId(snap.getId());
                            commentList.add(c);
                        }
                    }
                    if (tvNoComments != null) tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                    commentAdapter.notifyDataSetChanged();
                    if (rvComments != null && !commentList.isEmpty()) {
                        rvComments.post(() -> rvComments.smoothScrollToPosition(commentList.size() - 1));
                    }
                });

        if (btnSend != null) {
            btnSend.setOnClickListener(view -> {
                String content = etComment != null ? etComment.getText().toString().trim() : "";
                if (content.isEmpty()) return;

                DocumentReference newCommentRef = db.collection("posts").document(post.getPostId()).collection("comments").document();
                CommentModel cm = new CommentModel(newCommentRef.getId(), currentUid, currentUsername, currentUserProfilePic, content, System.currentTimeMillis());

                newCommentRef.set(cm).addOnSuccessListener(aVoid -> {
                    if (etComment != null) etComment.setText("");
                    db.collection("posts").document(post.getPostId()).update("commentsCount", FieldValue.increment(1));
                });
            });
        }
        dialog.show();
    }
}