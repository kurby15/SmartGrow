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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;

public class AddPlantBottomSheetActivity extends BottomSheetDialogFragment {

    // Existing Form Controls
    private EditText etName, etSpecies, etDate;
    private Spinner spinnerStatus;
    private Button btnSubmit;

    // 📸 New Image Upload Controls
    private MaterialCardView cardUploadImage;
    private LinearLayout layoutImagePlaceholder;
    private ImageView imgPlantPreview;

    // Ang lalagyan ng Uri para sa gallery path (Susi para sa Firebase Storage ng mga coders mo)
    private Uri selectedImageUri = null;

    public AddPlantBottomSheetActivity() {
        // Required empty public constructor
    }

    // 🖼️ 1. Launcher para sa Gallery Picker
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Ipakita ang larawan sa preview panel at itago ang text placeholder
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
                            // Ipakita ang kinuhang snapshot bitmap at itago ang placeholder
                            imgPlantPreview.setImageBitmap(photo);
                            imgPlantPreview.setVisibility(View.VISIBLE);
                            layoutImagePlaceholder.setVisibility(View.GONE);

                            // Note sa backend: Pwede nilang i-convert itong bitmap to URI kung gusto nilang i-save sa Cloud Storage
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_plant_sheet, container, false);

        // 🔗 Bind Existing View Elements
        etName = view.findViewById(R.id.et_add_plant_name);
        etSpecies = view.findViewById(R.id.et_add_plant_species);
        etDate = view.findViewById(R.id.et_add_plant_date);
        spinnerStatus = view.findViewById(R.id.spinner_add_plant_status);
        btnSubmit = view.findViewById(R.id.btn_submit_new_plant);

        // 🔗 Bind New Image Upload View Elements
        cardUploadImage = view.findViewById(R.id.card_upload_image);
        layoutImagePlaceholder = view.findViewById(R.id.layout_image_placeholder);
        imgPlantPreview = view.findViewById(R.id.img_plant_preview);

        // 🛠️ Setup default static selection choices para sa Health Status Spinner
        String[] healthOptions = {"Healthy", "Needs Attention", "Diseased"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, healthOptions);
        spinnerStatus.setAdapter(adapter);

        // 📅 Trigger standard Native Android Calendar View on Focus/Click
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

        // 🚀 Final submit hook endpoint para sa back-end developer
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

    // 🛠️ Pop-up Dialog window para mamili kung Camera o Gallery ang bubuksan
    private void showImageSourceOptions() {
        String[] options = {"Take Photo (Camera)", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Select Plant Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Fire Camera Hardware Intent Target Action
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else if (which == 1) {
                // Fire Native Media Gallery Explorer View Intent Action
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
        String healthStatus = spinnerStatus.getSelectedItem().toString();

        // Safe basic structural check validator before executing transactions
        if (plantName.isEmpty() || plantSpecies.isEmpty() || datePlanted.isEmpty()) {
            Toast.makeText(getContext(), "Please clear all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        /* 🚧 DEV NOTE (PARA SA BACKEND CODERS):
           Dito niyo na isasaksak yung upload integration.
           1. I-upload muna yung `selectedImageUri` (kung meron) sa Firebase Storage gamit ang storageRef.putFile().
           2. Kapag nakuha na yung image downloadUrl, isasama ito sa Realtime Database or Firestore object payload distribution.

           Halimbawa:
           DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Diaries");
           ref.push().setValue(new PlantModel(plantName, plantSpecies, datePlanted, healthStatus, downloadUrl));
        */

        Toast.makeText(getContext(), plantName + " added successfully!", Toast.LENGTH_SHORT).show();
        dismiss(); // Auto slide down pagkatapos mag-save
    }
}
