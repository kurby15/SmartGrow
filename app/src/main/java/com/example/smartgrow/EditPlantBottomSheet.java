package com.example.smartgrow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

    // Inayos ang mga Edit Texts kasama na ang Medicinal Use
    private EditText etEditName, etEditSpecies, etEditMedicinalUse, etEditDate;
    private MaterialButton btnSave;

    private MaterialCardView cardEditUploadImage;
    private LinearLayout layoutEditImagePlaceholder;
    private ImageView imgEditPlantPreview, imgEditCalendarIcon;

    private String plantId, currentName, currentSpecies, currentMedicinalUse, currentDate, currentStatus;
    private DatabaseReference databaseReference;
    private String currentUsername;

    // Dinagdag ang "medicinalUse" parameter sa bagong instance loader
    public static EditPlantBottomSheet newInstance(String plantId, String name, String species, String medicinalUse, String date, String status) {
        EditPlantBottomSheet fragment = new EditPlantBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_id", plantId);
        args.putString("key_name", name);
        args.putString("key_species", species);
        args.putString("key_medicinal", medicinalUse);
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
            currentMedicinalUse = getArguments().getString("key_medicinal");
            currentDate = getArguments().getString("key_date");

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
            // KONTROL PARA SA KEYBOARD: Pinipilit ang window na mag-adjust kapag lumabas ang soft keyboard
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Pigilan ang bottom sheet na mag-collapse kapag hinila paitaas ng keyboard
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_plant_sheet, container, false);

        // Bind ng text inputs kasama ang medicinal use
        etEditName = view.findViewById(R.id.et_edit_plant_name);
        etEditSpecies = view.findViewById(R.id.et_edit_plant_species);
        etEditMedicinalUse = view.findViewById(R.id.et_edit_plant_medicinal_use);
        etEditDate = view.findViewById(R.id.et_edit_plant_date);
        btnSave = view.findViewById(R.id.btn_save_plant_changes);

        cardEditUploadImage = view.findViewById(R.id.card_edit_upload_image);
        layoutEditImagePlaceholder = view.findViewById(R.id.layout_edit_image_placeholder);
        imgEditPlantPreview = view.findViewById(R.id.img_edit_plant_preview);
        imgEditCalendarIcon = view.findViewById(R.id.img_edit_calendar_icon);

        // Auto-fill ng active details
        etEditName.setText(currentName);
        etEditSpecies.setText(currentSpecies);
        etEditMedicinalUse.setText(currentMedicinalUse);
        etEditDate.setText(currentDate);

        // Date Picker setup
        View.OnClickListener datePickerListener = v -> showDatePickerDialog();
        etEditDate.setOnClickListener(datePickerListener);
        imgEditCalendarIcon.setOnClickListener(datePickerListener);

        // Upload handler mock
        cardEditUploadImage.setOnClickListener(v ->
                Toast.makeText(getContext(), "Opening Camera/Gallery for Photo Update...", Toast.LENGTH_SHORT).show()
        );

        // Save logic
        btnSave.setOnClickListener(v -> {
            String updatedName = etEditName.getText().toString().trim();
            String updatedSpecies = etEditSpecies.getText().toString().trim();
            String updatedMedicinal = etEditMedicinalUse.getText().toString().trim();
            String updatedDate = etEditDate.getText().toString().trim();

            if (updatedName.isEmpty() || updatedSpecies.isEmpty() || updatedMedicinal.isEmpty() || updatedDate.isEmpty()) {
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
            updates.put("medicinalUse", updatedMedicinal); // Ngayon ay mai-save na ito sa Firebase!
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