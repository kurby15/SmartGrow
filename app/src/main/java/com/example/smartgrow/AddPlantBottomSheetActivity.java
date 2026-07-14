package com.example.smartgrow;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color; // Siniguradong may import na ito para sa TRANSPARENT background
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;

public class AddPlantBottomSheetActivity extends BottomSheetDialogFragment {

    private EditText etName, etSpecies, etMedicinalUse, etDate;
    private TextView tvStaticStatus;
    private MaterialButton btnSubmit;

    private MaterialCardView cardUploadImage;
    private LinearLayout layoutImagePlaceholder;
    private ImageView imgPlantPreview;

    private DatabaseReference databaseReference;
    private String currentUsername;
    private Uri selectedImageUri = null;

    public AddPlantBottomSheetActivity() {
        // Required empty public constructor
    }

    public static AddPlantBottomSheetActivity newInstance() {
        return new AddPlantBottomSheetActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        if (!currentUsername.isEmpty()) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUsername).child("plants");
        }
    }

    // ⭐ FIXED DIALOG OVERLAY AND SLIDE PHYSICS (SUMUSUNOD NA SA STYLE NG EDIT)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            // I-enable ang dimming system flag
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Kontrol sa tapang ng background transparency layer (Kita ang 50% sa tuktok)
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.55f;
            dialog.getWindow().setAttributes(lp);

            // Soft keyboard adjust para hindi masira ang layout form kapag nag-type
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                // Naka-half sheet view profile state para laging litaw ang top background view
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);

                // Eksenang 65% ng screen ang taas para sakto lang sa form factor ng layout
                int displayHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (displayHeight * 0.65));

                // Burahin ang native framework layout dark overlay tint
                View parent = (View) bottomSheet.getParent();
                if (parent != null) {
                    parent.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        return dialog;
    }

    // 🖼️ Gallery Picker Launcher
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        imgPlantPreview.setImageURI(selectedImageUri);
                        imgPlantPreview.setVisibility(View.VISIBLE);
                        layoutImagePlaceholder.setVisibility(View.GONE);
                    }
                }
            });

    // 📷 Camera Launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        if (photo != null) {
                            imgPlantPreview.setImageBitmap(photo);
                            imgPlantPreview.setVisibility(View.VISIBLE);
                            layoutImagePlaceholder.setVisibility(View.GONE);
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_plant_sheet, container, false);

        // Bind Controls
        etName = view.findViewById(R.id.et_add_plant_name);
        etSpecies = view.findViewById(R.id.et_add_plant_species);
        etMedicinalUse = view.findViewById(R.id.et_add_plant_medicinal_use);
        etDate = view.findViewById(R.id.et_add_plant_date);
        tvStaticStatus = view.findViewById(R.id.tv_static_health_status);
        btnSubmit = view.findViewById(R.id.btn_submit_new_plant);

        cardUploadImage = view.findViewById(R.id.card_upload_image);
        layoutImagePlaceholder = view.findViewById(R.id.layout_image_placeholder);
        imgPlantPreview = view.findViewById(R.id.img_plant_preview);

        // Date Picker Handler
        View.OnClickListener dateListener = v -> showDatePicker();
        etDate.setOnClickListener(dateListener);

        // Image Selection Handler
        cardUploadImage.setOnClickListener(v -> showImageSourceOptions());

        // Submit Handler
        btnSubmit.setOnClickListener(v -> executePlantSubmission());

        return view;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etDate.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo (Camera)", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Select Plant Image Source");
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

    private void executePlantSubmission() {
        String plantName = etName.getText().toString().trim();
        String plantSpecies = etSpecies.getText().toString().trim();
        String medicinalUse = etMedicinalUse.getText().toString().trim();
        String datePlanted = etDate.getText().toString().trim();

        if (plantName.isEmpty() || plantSpecies.isEmpty() || datePlanted.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseReference == null) {
            Toast.makeText(getContext(), "Error: User session lost.", Toast.LENGTH_SHORT).show();
            return;
        }

        String plantId = databaseReference.push().getKey();
        if (plantId == null) return;

        PlantModel plant = new PlantModel(plantName, plantSpecies, datePlanted, "Healthy", medicinalUse, 100);
        plant.setId(plantId);

        if (selectedImageUri != null) {
            plant.setImageUrl(selectedImageUri.toString());
        }

        databaseReference.child(plantId).setValue(plant).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), plantName + " added successfully!", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to add plant.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}