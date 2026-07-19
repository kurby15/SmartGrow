package com.example.smartgrow;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class AddPlantBottomSheetActivity extends BottomSheetDialogFragment {

    private EditText etName, etSpecies, etMedicinalUse, etDate;
    private MaterialButton btnSubmit;
    private MaterialCardView cardUploadImage;
    private LinearLayout layoutImagePlaceholder;
    private ImageView imgPlantPreview;

    private DatabaseReference databaseReference;
    private String currentUsername;
    private Bitmap selectedBitmap = null;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setHideable(true);
            }
        });
        return dialog;
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(uri);
                        selectedBitmap = BitmapFactory.decodeStream(is);
                        imgPlantPreview.setImageBitmap(selectedBitmap);
                        imgPlantPreview.setVisibility(View.VISIBLE);
                        layoutImagePlaceholder.setVisibility(View.GONE);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    selectedBitmap = (Bitmap) extras.get("data");
                    imgPlantPreview.setImageBitmap(selectedBitmap);
                    imgPlantPreview.setVisibility(View.VISIBLE);
                    layoutImagePlaceholder.setVisibility(View.GONE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_plant_sheet, container, false);
        etName = view.findViewById(R.id.et_add_plant_name);
        etSpecies = view.findViewById(R.id.et_add_plant_species);
        etMedicinalUse = view.findViewById(R.id.et_add_plant_medicinal_use);
        etDate = view.findViewById(R.id.et_add_plant_date);
        btnSubmit = view.findViewById(R.id.btn_submit_new_plant);
        cardUploadImage = view.findViewById(R.id.card_upload_image);
        layoutImagePlaceholder = view.findViewById(R.id.layout_image_placeholder);
        imgPlantPreview = view.findViewById(R.id.img_plant_preview);

        etDate.setOnClickListener(v -> showDatePicker());
        cardUploadImage.setOnClickListener(v -> showImageSourceOptions());
        btnSubmit.setOnClickListener(v -> executePlantSubmission());

        return view;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            etDate.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Gallery", "Cancel"};
        new android.app.AlertDialog.Builder(requireContext()).setTitle("Select Photo")
            .setItems(options, (dialog, which) -> {
                if (which == 0) cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                else if (which == 1) galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            }).show();
    }

    private void executePlantSubmission() {
        String name = etName.getText().toString().trim();
        String species = etSpecies.getText().toString().trim();
        String medicinal = etMedicinalUse.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (name.isEmpty() || species.isEmpty() || date.isEmpty()) {
            Toast.makeText(getContext(), "Fill in required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        String base64Image = "";
        if (selectedBitmap != null) {
            // 🚀 Resize to save space in Realtime DB
            Bitmap resized = Bitmap.createScaledBitmap(selectedBitmap, 400, 400, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        }

        String id = databaseReference.push().getKey();
        PlantModel plant = new PlantModel(name, species, date, "Healthy", medicinal, 100);
        plant.setId(id);
        plant.setImageUrl(base64Image); // Text string na ang isesave natin

        databaseReference.child(id).setValue(plant).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Plant Added!", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                btnSubmit.setEnabled(true);
                Toast.makeText(getContext(), "Failed to save.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}