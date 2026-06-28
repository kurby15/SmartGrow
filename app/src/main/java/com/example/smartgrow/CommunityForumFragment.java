package com.example.smartgrow;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CommunityForumFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CommunityForumFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // UI Element para sa camera icon
    private ImageView btnCameraIcon;

    public CommunityForumFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // I-inflate ang layout ng fragment
        View view = inflater.inflate(R.layout.fragment_community_forum, container, false);

        // Hanapin ang camera button sa layout
        btnCameraIcon = view.findViewById(R.id.btn_camera_icon);

        // Click listener para bumukas ang slide-up bottom sheet
        if (btnCameraIcon != null) {
            btnCameraIcon.setOnClickListener(v -> showCreatePostDialog());
        }

        return view;
    }

    /**
     * Nagpapakita ng iOS-style Slide-Up Bottom Sheet para sa paggawa ng post
     */
    private void showCreatePostDialog() {
        if (getContext() == null) return;

        // 1. Gumawa ng BottomSheetDialog instance
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());

        // 2. I-set ang iyong custom layout sheet (dialog_create_post.xml)
        bottomSheetDialog.setContentView(R.layout.dialog_create_post);

        // 3. Gawing transparent ang default background ng sheet para lumabas ang rounded corners ng CardView mo
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundColor(Color.TRANSPARENT);
        }

        // 4. I-initialize ang mga UI elements sa loob ng dialog_create_post.xml
        EditText etCreatePostBox = bottomSheetDialog.findViewById(R.id.et_create_post_box);
        LinearLayout btnAddPhoto = bottomSheetDialog.findViewById(R.id.btn_add_photo);
        MaterialButton btnSubmitPost = bottomSheetDialog.findViewById(R.id.btn_submit_post);

        // Action para sa "Add Photo" layout sa loob ng sheet
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Opening Gallery...", Toast.LENGTH_SHORT).show();
                // TODO: Idagdag ang gallery intent o photo picker logic mo rito sa susunod
            });
        }

        // Action para sa "Post" button sa loob ng sheet
        if (btnSubmitPost != null) {
            btnSubmitPost.setOnClickListener(v -> {
                if (etCreatePostBox != null) {
                    String postContent = etCreatePostBox.getText().toString().trim();

                    if (postContent.isEmpty()) {
                        Toast.makeText(getContext(), "Please write something first!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Post Submitted Successfully!", Toast.LENGTH_SHORT).show();
                        // TODO: Idagdag ang database saving logic o API call mo rito

                        bottomSheetDialog.dismiss(); // Isasara ang sheet pagkatapos mag-post
                    }
                }
            });
        }

        // 5. Ipakita ang sliding dialog sa screen
        bottomSheetDialog.show();
    }
}