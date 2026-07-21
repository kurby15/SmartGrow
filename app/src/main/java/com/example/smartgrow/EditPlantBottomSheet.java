package com.example.smartgrow;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditPlantBottomSheet extends BottomSheetDialogFragment {

    private EditText etEditName, etEditSpecies, etEditMedicinalUse, etEditDate;
    private MaterialButton btnSave;

    private MaterialCardView cardEditUploadImage;
    private LinearLayout layoutEditImagePlaceholder;
    private ImageView imgEditPlantPreview, imgEditCalendarIcon;

    private String plantId, currentName, currentSpecies, currentMedicinalUse, currentDate, currentStatus, currentImageUrl;
    private DatabaseReference databaseReference;
    private String currentUsername;
    private Bitmap selectedBitmap = null;
    private SharedPrefManager prefManager;

    public static EditPlantBottomSheet newInstance(String plantId, String name, String species, String medicinalUse, String date, String status, String imageUrl) {
        EditPlantBottomSheet fragment = new EditPlantBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_id", plantId);
        args.putString("key_name", name);
        args.putString("key_species", species);
        args.putString("key_medicinal", medicinalUse);
        args.putString("key_date", date);
        args.putString("key_status", status);
        args.putString("key_image", imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔐 SECURE DATA FETCH
        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();

        if (getArguments() != null) {
            plantId = getArguments().getString("key_id");
            currentName = getArguments().getString("key_name");
            currentSpecies = getArguments().getString("key_species");
            currentMedicinalUse = getArguments().getString("key_medicinal");
            currentDate = getArguments().getString("key_date");
            currentImageUrl = getArguments().getString("key_image");

            if (currentDate != null && currentDate.contains("Planted: ")) {
                currentDate = currentDate.replace("Planted: ", "");
            }
            currentStatus = getArguments().getString("key_status");
        }

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown") && plantId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId);
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(uri);
                        selectedBitmap = BitmapFactory.decodeStream(is);
                        imgEditPlantPreview.setImageBitmap(selectedBitmap);
                        imgEditPlantPreview.setVisibility(View.VISIBLE);
                        layoutEditImagePlaceholder.setVisibility(View.GONE);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        selectedBitmap = (Bitmap) extras.get("data");
                        if (selectedBitmap != null) {
                            imgEditPlantPreview.setImageBitmap(selectedBitmap);
                            imgEditPlantPreview.setVisibility(View.VISIBLE);
                            layoutEditImagePlaceholder.setVisibility(View.GONE);
                        }
                    }
                }
            });

    private void showImageSourceOptions() {
        String[] options = {"Take Photo (Camera)", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Update Plant Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_plant_sheet, container, false);
        etEditName = view.findViewById(R.id.et_edit_plant_name);
        etEditSpecies = view.findViewById(R.id.et_edit_plant_species);
        etEditMedicinalUse = view.findViewById(R.id.et_edit_plant_medicinal_use);
        etEditDate = view.findViewById(R.id.et_edit_plant_date);
        btnSave = view.findViewById(R.id.btn_save_plant_changes);
        cardEditUploadImage = view.findViewById(R.id.card_edit_upload_image);
        layoutEditImagePlaceholder = view.findViewById(R.id.layout_edit_image_placeholder);
        imgEditPlantPreview = view.findViewById(R.id.img_edit_plant_preview);
        imgEditCalendarIcon = view.findViewById(R.id.img_edit_calendar_icon);

        etEditName.setText(currentName);
        etEditSpecies.setText(currentSpecies);
        etEditMedicinalUse.setText(currentMedicinalUse);
        etEditDate.setText(currentDate);

        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            if (currentImageUrl.startsWith("http")) {
                Glide.with(this).load(currentImageUrl).into(imgEditPlantPreview);
            } else {
                try {
                    byte[] decodedString = Base64.decode(currentImageUrl, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgEditPlantPreview.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    imgEditPlantPreview.setImageResource(R.drawable.smartgrow_logo);
                }
            }
            imgEditPlantPreview.setVisibility(View.VISIBLE);
            layoutEditImagePlaceholder.setVisibility(View.GONE);
        }

        View.OnClickListener datePickerListener = v -> showDatePickerDialog();
        etEditDate.setOnClickListener(datePickerListener);
        imgEditCalendarIcon.setOnClickListener(datePickerListener);
        cardEditUploadImage.setOnClickListener(v -> showImageSourceOptions());

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
                Toast.makeText(getContext(), "Session error. Please login again.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", updatedName);
            updates.put("species", updatedSpecies);
            updates.put("medicinalUse", updatedMedicinal);
            updates.put("datePlanted", updatedDate);

            if (selectedBitmap != null) {
                Bitmap resized = Bitmap.createScaledBitmap(selectedBitmap, 400, 400, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                updates.put("imageUrl", base64Image);
            }

            databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
                btnSave.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Changes Saved!", Toast.LENGTH_SHORT).show();
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
