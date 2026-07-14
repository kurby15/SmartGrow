package com.example.smartgrow;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {

    private ShapeableImageView imgUserAvatar;
    private TextView tvUsername, tvPostTarget;
    private EditText etPostBox;
    private LinearLayout btnAddPhoto;
    private MaterialButton btnSubmitPost;

    public static CreatePostBottomSheet newInstance() {
        return new CreatePostBottomSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;

            // 🚀 KEYBOARD COMPATIBILITY ENGINE: dynamic layout compression kung may lumabas na keyboard
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Lock expanded structure behavior parameters
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Buong lalabas para fit to viewport limits
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔗 Core Component Bindings
        imgUserAvatar = view.findViewById(R.id.img_create_post);
        tvUsername = view.findViewById(R.id.tv_identity_user_name);
        tvPostTarget = view.findViewById(R.id.tv_identity_post_target);
        etPostBox = view.findViewById(R.id.et_create_post_box);
        btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        btnSubmitPost = view.findViewById(R.id.btn_submit_post);

        // Media attachment logic callback hook
        btnAddPhoto.setOnClickListener(v -> {
            // I-execute ang gallery or image capture flow dito
        });

        // Submit action dispatcher
        btnSubmitPost.setOnClickListener(v -> {
            String postContent = etPostBox.getText().toString().trim();
            // Database dispatch integration map process here
        });
    }
}