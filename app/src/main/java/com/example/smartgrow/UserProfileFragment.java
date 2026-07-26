package com.example.smartgrow;

import android.app.AlertDialog;
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
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileFragment extends Fragment implements CommunityPostAdapter.OnPostInteractionListener {

    private String targetUsername;
    private String currentUsername;
    private TextView tvUsername, tvFullName, tvBio, tvNoPosts;
    private TextView tvFollowersCount, tvFollowingCount;
    private ImageView ivProfilePic;
    private View btnEditBio, btnSeeArchive, layoutFollowers, layoutFollowing;
    private com.google.android.material.button.MaterialButton btnFollow;
    private RecyclerView rvPosts;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private DatabaseReference userRef, postsRef, currentUserFollowingRef;

    public static UserProfileFragment newInstance(String username) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("target_username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUsername = getArguments().getString("target_username");
        }
        currentUsername = SharedPrefManager.getInstance(requireContext()).getUsername();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(targetUsername);
        currentUserFollowingRef = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("following");
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        tvUsername = view.findViewById(R.id.tv_username_display);
        tvFullName = view.findViewById(R.id.tv_full_name);
        tvBio = view.findViewById(R.id.tv_bio);
        tvNoPosts = view.findViewById(R.id.tv_no_posts);
        tvFollowersCount = view.findViewById(R.id.tv_followers_count);
        tvFollowingCount = view.findViewById(R.id.tv_following_count);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnEditBio = view.findViewById(R.id.btn_edit_bio);
        btnSeeArchive = view.findViewById(R.id.btn_see_archive);
        btnFollow = view.findViewById(R.id.btn_follow);
        rvPosts = view.findViewById(R.id.rv_user_posts);
        
        // Social Layouts
        layoutFollowers = view.findViewById(R.id.layout_followers_click);
        layoutFollowing = view.findViewById(R.id.layout_following_click);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (targetUsername.equals(currentUsername)) {
            btnEditBio.setVisibility(View.VISIBLE);
            btnSeeArchive.setVisibility(View.VISIBLE);
            btnFollow.setVisibility(View.GONE);
            btnEditBio.setOnClickListener(v -> showModernEditBioSheet());
            tvBio.setOnClickListener(v -> showModernEditBioSheet());
            btnSeeArchive.setOnClickListener(v -> openArchive());
        } else {
            btnEditBio.setVisibility(View.GONE);
            btnFollow.setVisibility(View.VISIBLE);
            checkIfFollowing();
            btnFollow.setOnClickListener(v -> toggleFollow());
        }

        // Click listeners for social lists
        if (layoutFollowers != null) layoutFollowers.setOnClickListener(v -> showUserListSheet("Followers", "followers"));
        if (layoutFollowing != null) layoutFollowing.setOnClickListener(v -> showUserListSheet("Following", "following"));

        setupRecyclerView();
        fetchUserData();
        fetchUserPosts();

        return view;
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
            userRef.child("bio").setValue(newBio).addOnSuccessListener(aVoid -> {
                sheet.dismiss();
                Toast.makeText(getContext(), "Bio updated! 🌿", Toast.LENGTH_SHORT).show();
            });
        });

        sheet.show();
    }

    private void showUserListSheet(String title, String nodeName) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_user_list_sheet, null);
        sheet.setContentView(v);

        // Make it full screenish
        View bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            bottomSheet.getLayoutParams().height = (int)(screenHeight * 0.8);
        }

        TextView tvTitle = v.findViewById(R.id.tv_list_title);
        tvTitle.setText(title);
        
        ProgressBar pb = v.findViewById(R.id.pb_loading);
        TextView tvEmpty = v.findViewById(R.id.tv_empty_message);
        RecyclerView rv = v.findViewById(R.id.rv_user_list);
        
        List<User> userList = new ArrayList<>();
        UserListAdapter userAdapter = new UserListAdapter(userList, username -> {
            sheet.dismiss();
            onUserClick(username);
        });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(userAdapter);

        pb.setVisibility(View.VISIBLE);
        userRef.child(nodeName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    pb.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                int total = (int) snapshot.getChildrenCount();
                final int[] loaded = {0};

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String uid = snap.getKey();
                    FirebaseDatabase.getInstance().getReference("users").child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    User u = userSnap.getValue(User.class);
                                    if (u != null) userList.add(u);
                                    loaded[0]++;
                                    if (loaded[0] == total) {
                                        pb.setVisibility(View.GONE);
                                        userAdapter.notifyDataSetChanged();
                                        tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pb.setVisibility(View.GONE); }
        });

        sheet.show();
    }

    private void checkIfFollowing() {
        currentUserFollowingRef.child(targetUsername).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    btnFollow.setText("Unfollow");
                    btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
                } else {
                    btnFollow.setText("Follow");
                    btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0C6211")));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleFollow() {
        btnFollow.setEnabled(false);
        currentUserFollowingRef.child(targetUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    unfollowUser();
                } else {
                    followUser();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { btnFollow.setEnabled(true); }
        });
    }

    private void followUser() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + currentUsername + "/following/" + targetUsername, true);
        updates.put("users/" + targetUsername + "/followers/" + currentUsername, true);
        
        FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
            btnFollow.setEnabled(true);
            if (task.isSuccessful()) {
                updateFollowCounts(1);
            }
        });
    }

    private void unfollowUser() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + currentUsername + "/following/" + targetUsername, null);
        updates.put("users/" + targetUsername + "/followers/" + currentUsername, null);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
            btnFollow.setEnabled(true);
            if (task.isSuccessful()) {
                updateFollowCounts(-1);
            }
        });
    }

    private void updateFollowCounts(int increment) {
        FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("followingCount").runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData md) {
                Long current = md.getValue(Long.class);
                if (current == null) current = 0L;
                md.setValue(Math.max(0, current + increment));
                return Transaction.success(md);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean b, @Nullable DataSnapshot ds) {}
        });

        userRef.child("followersCount").runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData md) {
                Long current = md.getValue(Long.class);
                if (current == null) current = 0L;
                md.setValue(Math.max(0, current + increment));
                return Transaction.success(md);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean b, @Nullable DataSnapshot ds) {}
        });
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
        adapter = new CommunityPostAdapter(postList, currentUsername, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setAdapter(adapter);
    }

    private void fetchUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvUsername.setText("@" + user.getUsername());
                    tvFullName.setText(user.getFullName());
                    tvFollowersCount.setText(String.valueOf(user.getFollowersCount()));
                    tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));
                    
                    String bioText = user.getBio();
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadProfileImage(String profileData) {
        if (profileData == null || profileData.isEmpty()) {
            ivProfilePic.setImageResource(R.drawable.ic_user);
            return;
        }
        try {
            byte[] decodedString = Base64.decode(profileData, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivProfilePic.setImageBitmap(bitmap);
        } catch (Exception e) {
            ivProfilePic.setImageResource(R.drawable.ic_user);
        }
    }

    private void fetchUserPosts() {
        postsRef.orderByChild("username").equalTo(targetUsername).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                postList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    CommunityPostModel post = snap.getValue(CommunityPostModel.class);
                    if (post != null) {
                        post.setPostId(snap.getKey());
                        postList.add(post);
                    }
                }
                Collections.sort(postList, (p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvNoPosts.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override public void onLikeClick(CommunityPostModel post) {
        postsRef.child(post.getPostId()).runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData md) {
                CommunityPostModel p = md.getValue(CommunityPostModel.class);
                if (p == null) return Transaction.success(md);
                Map<String, Boolean> likes = p.getLikes();
                if (likes == null) likes = new HashMap<>();
                if (likes.containsKey(currentUsername)) {
                    p.setLikesCount(Math.max(0, p.getLikesCount() - 1));
                    likes.remove(currentUsername);
                } else {
                    p.setLikesCount(p.getLikesCount() + 1);
                    likes.put(currentUsername, true);
                }
                p.setLikes(likes);
                md.setValue(p);
                return Transaction.success(md);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean b, @Nullable DataSnapshot ds) {}
        });
    }

    @Override public void onCommentClick(CommunityPostModel post) {
        Toast.makeText(getContext(), "Open comments from main forum", Toast.LENGTH_SHORT).show();
    }

    @Override public void onUserClick(String username) {
        if (!username.equals(targetUsername)) {
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

        if (post.getUserId() != null && post.getUserId().equals(currentUsername)) {
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
            postsRef.child(post.getPostId()).removeValue();
            FirebaseDatabase.getInstance().getReference("comments").child(post.getPostId()).removeValue();
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
            if (post.getPostImageUri().length() > 1000) {
                byte[] decodedString = Base64.decode(post.getPostImageUri(), Base64.DEFAULT);
                ivPreview.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
            } else {
                Glide.with(this).load(post.getPostImageUri()).into(ivPreview);
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

            postsRef.child(post.getPostId()).updateChildren(updates).addOnSuccessListener(aVoid -> {
                dialog.dismiss();
                Toast.makeText(getContext(), "Post updated! ✨", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                btnUpdate.setEnabled(true);
                btnUpdate.setText("Update Post");
                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }
}