package com.example.smartgrow;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;

public class AddPlantBottomSheetActivity extends BottomSheetDialogFragment {

    // Existing Form Controls
    private EditText etName, etSpecies, etDate;
    private TextView tvStaticStatus; // 🌟 PINALITAN NA ANG SPINNER NG TEXTVIEW!
    private MaterialButton btnSubmit;

    // 📸 Image Upload Controls
    private MaterialCardView cardUploadImage;
    private LinearLayout layoutImagePlaceholder;
    private ImageView imgPlantPreview;

    // Ang lalagyan ng Uri para sa gallery path
    private Uri selectedImageUri = null;

    public AddPlantBottomSheetActivity() {
        // Required empty public constructor
    }

    public static AddPlantBottomSheetActivity  newInstance() {
        return new AddPlantBottomSheetActivity ();
    }

    // 🖼️ 1. Launcher para sa Gallery Picker
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

    // 📷 2. Launcher para sa Camera Application
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
        // 🌟 SIGURADUHING NAKAKABIT DITO YUNG PINAKABAGONG XML NATIN!
        View view = inflater.inflate(R.layout.dialog_add_plant_sheet, container, false);

        // 🔗 Bind View Elements
        etName = view.findViewById(R.id.et_add_plant_name);
        etSpecies = view.findViewById(R.id.et_add_plant_species);
        etDate = view.findViewById(R.id.et_add_plant_date);
        tvStaticStatus = view.findViewById(R.id.tv_static_health_status); // 🔗 Bounded sa bagong badge text natin!
        btnSubmit = view.findViewById(R.id.btn_submit_new_plant);

        // 📸 Bind Image Upload Elements
        cardUploadImage = view.findViewById(R.id.card_upload_image);
        layoutImagePlaceholder = view.findViewById(R.id.layout_image_placeholder);
        imgPlantPreview = view.findViewById(R.id.img_plant_preview);

        // 📅 Trigger standard Native Android Calendar View on Click
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // 📸 Trigger Image Picker Selection Dialog
        cardUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceOptions();
            }
        });

        // 🚀 Submit hook handler
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePlantSubmission();
            }
        });

        return view;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        etDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo (Camera)", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
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
        String datePlanted = etDate.getText().toString().trim();

        // Automatic "Healthy" na string ang ipapasa sa Firebase database base sa static view natin!
        String healthStatus = "Healthy";

        if (plantName.isEmpty() || plantSpecies.isEmpty() || datePlanted.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        /* 🚧 DEV NOTE (PARA SA INYONG BACKEND):
           Ipasok ang `healthStatus` ("Healthy") diretso sa Firebase ref submission object payload niyo.
        */

        Toast.makeText(getContext(), plantName + " added successfully!", Toast.LENGTH_SHORT).show();
        dismiss(); // Eto ang magpapadulas/slide down sa kaniya pababa nang kusa pagkatapos mag-save!
    }
}