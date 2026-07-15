package com.example.smartgrow;

import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Context;
import android.content.SharedPreferences;

public class CommunityForumFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private MaterialCardView cardMind;
    private ImageView btnCameraIcon;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri = null;

    public CommunityForumFragment() {
        // Required empty public constructor
    }

    public static CommunityForumFragment newInstance(String param1, String param2) {
        CommunityForumFragment fragment = new CommunityForumFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        // Diretsong bubuksan ang dialog at ipapakita ang piniling larawan
                        showCreatePostDialog(true);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_forum, container, false);

        cardMind = view.findViewById(R.id.card_mind);
        if (cardMind != null) {
            cardMind.setOnClickListener(v -> showCreatePostDialog(false));
        }

        btnCameraIcon = view.findViewById(R.id.btn_camera_icon);
        if (btnCameraIcon != null) {
            btnCameraIcon.setOnClickListener(v -> {
                imagePickerLauncher.launch("image/*");
            });
        }

        recyclerView = view.findViewById(R.id.rv_forum_feed);
        postList = new ArrayList<>();
        adapter = new CommunityPostAdapter(postList);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
        }

        loadInitialPost();

        return view;
    }

    private void showCreatePostDialog(boolean autoSelectedImage) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_post, null);
        bottomSheetDialog.setContentView(dialogView);

        // Standard inputs at buttons
        EditText etPostContent = dialogView.findViewById(R.id.et_create_post_box);
        View btnAddPhoto = dialogView.findViewById(R.id.btn_add_photo);
        Button btnPost = dialogView.findViewById(R.id.btn_submit_post);

        // BAGONG BINDINGS: Kinuha natin ang references ng Preview Card elements mula sa bagong XML
        MaterialCardView cardPreviewContainer = dialogView.findViewById(R.id.card_preview_container);
        ImageView ivPostPreview = dialogView.findViewById(R.id.iv_post_preview);
        ImageButton btnRemovePhoto = dialogView.findViewById(R.id.btn_remove_photo);

        // CHECKER: Kung may napili nang image bago buksan ang dialog
        if (selectedImageUri != null && cardPreviewContainer != null && ivPostPreview != null) {
            ivPostPreview.setImageURI(selectedImageUri);
            cardPreviewContainer.setVisibility(View.VISIBLE); // Ipakita ang photo preview
        } else if (cardPreviewContainer != null) {
            cardPreviewContainer.setVisibility(View.GONE); // Itago kung walang image
        }

        // PINDUTAN NG REMOVE PHOTO (Yung "x" icon sa preview)
        if (btnRemovePhoto != null && cardPreviewContainer != null) {
            btnRemovePhoto.setOnClickListener(v -> {
                selectedImageUri = null; // Burahin ang hawak na URI
                cardPreviewContainer.setVisibility(View.GONE); // I-gone ang container para sumunod ang ibang UI elements sa taas
            });
        }

        // PINDUTAN NG ADD PHOTO SA LOOB NG DIALOG
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> {
                // Isasara muna ang dialog bago ilunsad ang gallery para iwas double-open bugs
                bottomSheetDialog.dismiss();
                imagePickerLauncher.launch("image/*");
            });
        }

        // POST SUBMIT BUTTON
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                try {
                    String content = etPostContent != null ? etPostContent.getText().toString().trim() : "";

                    if (content.isEmpty() && selectedImageUri == null) {
                        Toast.makeText(getContext(), "Please write something first...", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String loggedInName = getLoggedInUserFullName();
                    String formattedTime = getFormattedCurrentTime();
                    String imageStringPath = (selectedImageUri != null) ? selectedImageUri.toString() : null;

                    CommunityPostModel newPost = new CommunityPostModel(
                            "id_" + System.currentTimeMillis(),
                            loggedInName,
                            null,
                            formattedTime,
                            content,
                            imageStringPath,
                            0,
                            0
                    );

                    if (postList == null) {
                        postList = new ArrayList<>();
                    }

                    postList.add(0, newPost);

                    if (adapter != null) {
                        adapter.notifyItemInserted(0);
                    }

                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(0);
                    }

                    // I-clear ang image state matapos makapag-post
                    selectedImageUri = null;
                    bottomSheetDialog.dismiss();

                    Toast.makeText(getContext(), "Post submitted successfully!", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // Kapag dinismiss ang dialog nang hindi nag-popost
        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (!autoSelectedImage) {
                selectedImageUri = null;
            }
        });

        bottomSheetDialog.show();
    }

    private String getFormattedCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getLoggedInUserFullName() {
        if (getContext() == null) return "User Dev";

        SharedPreferences preferences = getContext().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        return preferences.getString("user_fullname", "Active User");
    }

    private void loadInitialPost() {
        postList.add(new CommunityPostModel(
                "test_init_1",
                "Rue Sabino",
                null,
                "2 hours ago",
                "My Lagundi plant has been growing really well this month! Highly recommend using organic compost.",
                null,
                65,
                44
        ));
        adapter.notifyDataSetChanged();
    }
}