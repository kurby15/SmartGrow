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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityForumFragment extends Fragment implements CommunityPostAdapter.OnPostInteractionListener {

    private RecyclerView recyclerView;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private MaterialCardView cardMind;
    private ImageView imgUserAvatar, btnMyProfile;
    private SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseReference postsRef, commentsRef;
    private FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri = null;
    private String currentUserId, currentUserFullName, currentUserProfilePic;
    private String detectedLocation = ""; 
    private String savedDraftContent = ""; 

    public CommunityForumFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        commentsRef = FirebaseDatabase.getInstance().getReference("comments");
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
        currentUserId = prefManager.getUsername();
        currentUserFullName = prefManager.getFullName();
        currentUserProfilePic = prefManager.getProfilePic();
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
            btnMyProfile.setOnClickListener(v -> onUserClick(currentUserId));
        }

        if (cardMind != null) {
            cardMind.setOnClickListener(v -> {
                selectedImageUri = null;
                savedDraftContent = "";
                showCreatePostDialog();
            });
        }
        
        if (imgUserAvatar != null) {
            imgUserAvatar.setOnClickListener(v -> onUserClick(currentUserId));
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
        adapter = new CommunityPostAdapter(postList, currentUserId, this);
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
        
        FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("hiddenPosts")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot hiddenSnapshot) {
                    Map<String, Boolean> hiddenIds = new HashMap<>();
                    if (hiddenSnapshot.exists()) {
                        for (DataSnapshot h : hiddenSnapshot.getChildren()) {
                            hiddenIds.put(h.getKey(), true);
                        }
                    }

                    postsRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!isAdded()) return;
                            postList.clear();
                            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                CommunityPostModel post = postSnapshot.getValue(CommunityPostModel.class);
                                if (post != null) {
                                    post.setPostId(postSnapshot.getKey()); 
                                    if (!post.isArchived() && !hiddenIds.containsKey(post.getPostId())) {
                                        postList.add(post);
                                    }
                                }
                            }
                            Collections.sort(postList, (p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                            adapter.notifyDataSetChanged();
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false); }
                    });
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
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

        if (tvName != null) tvName.setText(currentUserId); 
        if (imgAvatar != null) loadProfileImage(currentUserProfilePic, imgAvatar);
        if (etContent != null) etContent.setText(savedDraftContent);
        if (selectedImageUri != null) {
            if (ivPostPreview != null) ivPostPreview.setImageURI(selectedImageUri);
            if (cardPreview != null) cardPreview.setVisibility(View.VISIBLE);
        }

        View btnAddPhoto = v.findViewById(R.id.btn_add_photo);
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(view -> {
                savedDraftContent = etContent.getText().toString();
                imagePickerLauncher.launch("image/*");
                dialog.dismiss();
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(view -> {
                String content = etContent.getText().toString().trim();
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
        String postId = postsRef.push().getKey();
        CommunityPostModel post = new CommunityPostModel(postId, currentUserId, currentUserId, currentUserProfilePic, System.currentTimeMillis(), content, imageBase64, detectedLocation);
        if (postId != null) postsRef.child(postId).setValue(post).addOnSuccessListener(aVoid -> {
            dialog.dismiss();
            showSuccessDialog();
            selectedImageUri = null;
            savedDraftContent = "";
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Post failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Button btn = dialog.findViewById(R.id.btn_submit_post);
            if (btn != null) { btn.setEnabled(true); btn.setText("Post"); }
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
        postsRef.child(post.getPostId()).runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData md) {
                CommunityPostModel p = md.getValue(CommunityPostModel.class);
                if (p == null) return Transaction.success(md);
                Map<String, Boolean> likes = p.getLikes();
                if (likes == null) likes = new HashMap<>();
                if (likes.containsKey(currentUserId)) {
                    p.setLikesCount(Math.max(0, p.getLikesCount() - 1));
                    likes.remove(currentUserId);
                } else {
                    p.setLikesCount(p.getLikesCount() + 1);
                    likes.put(currentUserId, true);
                }
                p.setLikes(likes);
                md.setValue(p);
                return Transaction.success(md);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean b, @Nullable DataSnapshot ds) {}
        });
    }

    @Override public void onCommentClick(CommunityPostModel post) { showCommentsDialog(post); }

    @Override public void onUserClick(String username) {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, UserProfileFragment.newInstance(username))
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override public void onMoreClick(View v, CommunityPostModel post) {
        if (getContext() == null) return;
        
        BottomSheetDialog optionsSheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_options_sheet, null);
        optionsSheet.setContentView(sheetView);

        LinearLayout layoutOwner = sheetView.findViewById(R.id.layout_owner_options);
        LinearLayout layoutOther = sheetView.findViewById(R.id.layout_other_user_options);

        if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
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
                postsRef.child(post.getPostId()).child("archived").setValue(true).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post moved to Archive.", Toast.LENGTH_SHORT).show();
                });
            });
        }

        View itemMoveTrash = sheetView.findViewById(R.id.item_move_trash);
        if (itemMoveTrash != null) {
            itemMoveTrash.setOnClickListener(view -> {
                optionsSheet.dismiss();
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Move to Trash?")
                    .setMessage("This post will be permanently deleted after 30 days.")
                    .setPositiveButton("Move", (d, w) -> {
                        postsRef.child(post.getPostId()).removeValue();
                        commentsRef.child(post.getPostId()).removeValue();
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
                FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId).child("hiddenPosts").child(post.getPostId()).setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Post hidden. You won't see this again.", Toast.LENGTH_SHORT).show();
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
                if (post.getPostImageUri().length() > 1000) {
                    byte[] decodedString = Base64.decode(post.getPostImageUri(), Base64.DEFAULT);
                    ivPreview.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                } else {
                    Glide.with(this).load(post.getPostImageUri()).into(ivPreview);
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

                postsRef.child(post.getPostId()).updateChildren(updates).addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Post updated! ✨", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
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

        commentsRef.child(post.getPostId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (pb != null) pb.setVisibility(View.GONE);
                commentList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    CommentModel c = snap.getValue(CommentModel.class);
                    if (c != null) {
                        c.setCommentId(snap.getKey());
                        commentList.add(c);
                    }
                }
                if (tvNoComments != null) tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                commentAdapter.notifyDataSetChanged();
                if (rvComments != null && !commentList.isEmpty()) {
                    rvComments.post(() -> rvComments.smoothScrollToPosition(commentList.size() - 1));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { if (pb != null) pb.setVisibility(View.GONE); }
        });

        if (btnSend != null) {
            btnSend.setOnClickListener(view -> {
                String content = etComment != null ? etComment.getText().toString().trim() : "";
                if (content.isEmpty()) return;
                String cid = commentsRef.child(post.getPostId()).push().getKey();
                CommentModel cm = new CommentModel(cid, currentUserId, currentUserId, currentUserProfilePic, content, System.currentTimeMillis());
                if (cid != null) commentsRef.child(post.getPostId()).child(cid).setValue(cm).addOnSuccessListener(aVoid -> {
                    if (etComment != null) etComment.setText("");
                    updateCommentCount(post.getPostId());
                });
            });
        }
        dialog.show();
    }

    private void updateCommentCount(String postId) {
        postsRef.child(postId).runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData md) {
                CommunityPostModel p = md.getValue(CommunityPostModel.class);
                if (p == null) return Transaction.success(md);
                p.setCommentsCount(p.getCommentsCount() + 1);
                md.setValue(p);
                return Transaction.success(md);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean b, @Nullable DataSnapshot d) {}
        });
    }
}
