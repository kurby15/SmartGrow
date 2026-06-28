package com.example.smartgrow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditPlantBottomSheet extends BottomSheetDialogFragment {

    private EditText etEditName, etEditSpecies, etEditDate;
    private MaterialButton btnSave;

    // 📸 Mga UI components para sa Photo Section at Calendar Icon
    private MaterialCardView cardEditUploadImage;
    private LinearLayout layoutEditImagePlaceholder;
    private ImageView imgEditPlantPreview, imgEditCalendarIcon;

    // Kunin ang mga default values na pinasa mula sa card list
    private String plantId, currentName, currentSpecies, currentDate, currentStatus;
    private DatabaseReference databaseReference;
    private String currentUsername;

    public static EditPlantBottomSheet newInstance(String plantId, String name, String species, String date, String status) {
        EditPlantBottomSheet fragment = new EditPlantBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_id", plantId);
        args.putString("key_name", name);
        args.putString("key_species", species);
        args.putString("key_date", date);
        args.putString("key_status", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        if (getArguments() != null) {
            plantId = getArguments().getString("key_id");
            currentName = getArguments().getString("key_name");
            currentSpecies = getArguments().getString("key_species");
            currentDate = getArguments().getString("key_date");

            // ✂️ Linisin ang "Planted: " prefix kung meron man para malinis ang date sa input field
            if (currentDate != null && currentDate.contains("Planted: ")) {
                currentDate = currentDate.replace("Planted: ", "");
            }
            currentStatus = getArguments().getString("key_status");
        }

        if (!currentUsername.isEmpty() && plantId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId);
        }
    }

    // 🚀 CONTROL PARA SA TOUCH GESTURES (SLIDE UP / SLIDE DOWN DISMISS)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Setup ang native smooth slide animations ng Material Components
        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
        }

        // I-activate ang swipe down detection sa touchscreen
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true); // Pinapayagang ma-swipe pababa para mag-close
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Sapilitang naka-full bukas agad
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_plant_sheet, container, false);

        // 🔗 Core Fields Layout Binding
        etEditName = view.findViewById(R.id.et_edit_plant_name);
        etEditSpecies = view.findViewById(R.id.et_edit_plant_species);
        etEditDate = view.findViewById(R.id.et_edit_plant_date);
        btnSave = view.findViewById(R.id.btn_save_plant_changes);

        // 🔗 New Upload Layout & Icons Binding
        cardEditUploadImage = view.findViewById(R.id.card_edit_upload_image);
        layoutEditImagePlaceholder = view.findViewById(R.id.layout_edit_image_placeholder);
        imgEditPlantPreview = view.findViewById(R.id.img_edit_plant_preview);
        imgEditCalendarIcon = view.findViewById(R.id.img_edit_calendar_icon);

        // 🌟 AUTO-FILL: Isalpak agad ang lumang data sa mga textboxes
        etEditName.setText(currentName);
        etEditSpecies.setText(currentSpecies);
        etEditDate.setText(currentDate);

        // 📅 SCRIPT 1: Native DatePicker popup para sa Touch Screen
        View.OnClickListener datePickerListener = v -> showDatePickerDialog();
        etEditDate.setOnClickListener(datePickerListener);
        imgEditCalendarIcon.setOnClickListener(datePickerListener);

        // 📸 SCRIPT 2: Placeholder click listener para sa Camera/Gallery Upload Frame
        cardEditUploadImage.setOnClickListener(v ->
                Toast.makeText(getContext(), "Opening Camera/Gallery for Photo Update...", Toast.LENGTH_SHORT).show()
        );

        // 💾 SCRIPT 3: Save Button Action Hook
        btnSave.setOnClickListener(v -> {
            String updatedName = etEditName.getText().toString().trim();
            String updatedSpecies = etEditSpecies.getText().toString().trim();
            String updatedDate = etEditDate.getText().toString().trim();

            if (updatedName.isEmpty() || updatedSpecies.isEmpty() || updatedDate.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseReference == null) {
                Toast.makeText(getContext(), "Error: Database reference not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", updatedName);
            updates.put("species", updatedSpecies);
            updates.put("datePlanted", updatedDate);

            databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Changes Saved for " + updatedName + "!", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to update plant.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    // Helper function para sa tunay na Calendar Wheel interface ng phone
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etEditDate.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }
}