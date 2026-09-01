package com.example.smartgrow.plants;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.smartgrow.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class PlantNameBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_CURRENT_NAME = "current_plant_name";
    private static final String ARG_SUGGESTIONS = "plant_name_suggestions";

    private EditText etPlantName;
    private TextView tvTitle;
    private ImageView btnClose;
    private Button btnConfirm;
    private ChipGroup chipGroup;

    private OnPlantNameUpdatedListener listener;

    public interface OnPlantNameUpdatedListener {
        void onPlantNameUpdated(String newName);
    }

    /**
     * Standard factory method with plant name
     */
    public static PlantNameBottomSheetFragment newInstance(String currentName) {
        return newInstance(currentName, new ArrayList<>());
    }

    /**
     * Factory method supporting dynamic commonly called suggestions from PlantAnalyzer
     */
    public static PlantNameBottomSheetFragment newInstance(String currentName, List<String> suggestions) {
        PlantNameBottomSheetFragment fragment = new PlantNameBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_NAME, currentName);
        if (suggestions != null) {
            args.putStringArrayList(ARG_SUGGESTIONS, new ArrayList<>(suggestions));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPlantNameUpdatedListener(OnPlantNameUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Make sheet expand to FULL SCREEN without referencing internal R.id symbols
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            View view = getView();
            if (view != null) {
                View parent = (View) view.getParent();
                if (parent != null) {
                    parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    parent.setBackgroundColor(Color.TRANSPARENT);

                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_name_plant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPlantName = view.findViewById(R.id.etPlantName);
        tvTitle = view.findViewById(R.id.tvTitle);
        btnClose = view.findViewById(R.id.btnClose);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        chipGroup = view.findViewById(R.id.chipGroup);

        // Pre-fill current plant name and setup dynamic suggestions
        if (getArguments() != null) {
            String currentName = getArguments().getString(ARG_CURRENT_NAME, "");
            ArrayList<String> suggestions = getArguments().getStringArrayList(ARG_SUGGESTIONS);

            // Clean parentheses if scientific name is attached (e.g. "Venus Flytrap (Dionaea muscipula)" -> "Venus Flytrap")
            if (currentName != null && currentName.contains("(")) {
                currentName = currentName.substring(0, currentName.indexOf("(")).trim();
            }

            // Fallback to top suggestion ONLY if currentName is null or completely empty
            if ((currentName == null || currentName.trim().isEmpty()) && suggestions != null && !suggestions.isEmpty()) {
                currentName = suggestions.get(0);
            }

            etPlantName.setText(currentName);
            if (currentName != null && !currentName.isEmpty()) {
                etPlantName.setSelection(currentName.length());
            }

            // Populate dynamic chips from Gemini AI suggestions if available
            if (suggestions != null && !suggestions.isEmpty() && chipGroup != null) {
                populateDynamicChips(suggestions);
            } else {
                setupChipClickListeners();
            }
        } else {
            setupChipClickListeners();
        }

        // Close button listener
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        // Confirm button action
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String newName = etPlantName.getText().toString().trim();
                if (newName.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a plant name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listener != null) {
                    listener.onPlantNameUpdated(newName);
                }
                dismiss();
            });
        }
    }

    /**
     * Dynamically populates the ChipGroup with commonly called name suggestions parsed from Gemini
     */
    private void populateDynamicChips(List<String> suggestions) {
        if (chipGroup == null || getContext() == null) return;

        chipGroup.removeAllViews();
        for (String name : suggestions) {
            if (name == null || name.trim().isEmpty()) continue;

            String cleanName = name.trim();
            if (cleanName.contains("(")) {
                cleanName = cleanName.substring(0, cleanName.indexOf("(")).trim();
            }

            Chip chip = new Chip(getContext());
            chip.setText(cleanName);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                etPlantName.setText(chip.getText());
                etPlantName.setSelection(chip.getText().length());
            });

            chipGroup.addView(chip);
        }
    }

    /**
     * Attaches click listeners to pre-existing XML chips inside ChipGroup (fallback)
     */
    private void setupChipClickListeners() {
        if (chipGroup == null) return;

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    etPlantName.setText(chip.getText());
                    etPlantName.setSelection(chip.getText().length());
                });
            }
        }
    }
}