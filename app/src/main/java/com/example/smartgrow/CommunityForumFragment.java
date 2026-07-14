package com.example.smartgrow;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class CommunityForumFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // UI Elements
    private MaterialCardView cardMind;
    private ImageView btnCameraIcon;
    private RecyclerView rvForumFeed;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_forum, container, false);

        // I-initialize ang mga views
        cardMind = view.findViewById(R.id.card_mind);
        btnCameraIcon = view.findViewById(R.id.btn_camera_icon);
        rvForumFeed = view.findViewById(R.id.rv_forum_feed);

        // Responsive Action: Pindutin man ang buong card o ang maliit na camera icon, gagana ang Bottom Sheet dialog.
        View.OnClickListener openDialogListener = v -> showCreatePostDialog();

        if (cardMind != null) {
            cardMind.setOnClickListener(openDialogListener);
        }
        if (btnCameraIcon != null) {
            btnCameraIcon.setOnClickListener(openDialogListener);
        }

        // TODO: I-setup ang iyong RecyclerView Adapter dito para sa rvForumFeed

        return view;
    }

    /**
     * Nagpapakita ng Full-Responsive Slide-Up Bottom Sheet para sa paggawa ng post
     */
    private void showCreatePostDialog() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.dialog_create_post);

        // Gawing transparent ang background para lumabas ang setup ng rounded corner cards mo
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);
        }

        EditText etCreatePostBox = bottomSheetDialog.findViewById(R.id.et_create_post_box);
        LinearLayout btnAddPhoto = bottomSheetDialog.findViewById(R.id.btn_add_photo);
        MaterialButton btnSubmitPost = bottomSheetDialog.findViewById(R.id.btn_submit_post);

        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Opening Gallery...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSubmitPost != null) {
            btnSubmitPost.setOnClickListener(v -> {
                if (etCreatePostBox != null) {
                    String postContent = etCreatePostBox.getText().toString().trim();

                    if (postContent.isEmpty()) {
                        Toast.makeText(getContext(), "Please write something first!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Post Submitted Successfully!", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                    }
                }
            });
        }

        bottomSheetDialog.show();
    }
}