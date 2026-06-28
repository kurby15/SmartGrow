package com.example.smartgrow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddLogBottomSheet extends BottomSheetDialogFragment {

    private EditText etLogDate, etLogNotes;
    private ImageView imgCalendarIcon;
    private RadioGroup rgWatered, rgFertilized, rgSunlight, rgHealth;
    private MaterialButton btnSubmitLog;

    private String plantId;
    private DatabaseReference databaseReference;
    private String currentUsername;

    public static AddLogBottomSheet newInstance(String plantId) {
        AddLogBottomSheet fragment = new AddLogBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_plant_id", plantId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            plantId = getArguments().getString("key_plant_id");
        }

        SharedPreferences preferences = getActivity().getSharedPreferences("SmartGrowPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("current_username", "");

        if (!currentUsername.isEmpty() && plantId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId).child("logs");
        }
    }

    // ⭐ HETO ANG LOGIC: Pinapayagan si user mag slide up at slide down via Touchscreen gesture!
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = com.google.android.material.R.style.Animation_Material3_BottomSheetDialog;
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true); // Pwedeng hilahin pababa gamit ang daliri para i-dismiss!
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Automatic naka-open agad full views
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_log_sheet, container, false);

        // Bind interactive views
        etLogDate = view.findViewById(R.id.et_log_date);
        etLogNotes = view.findViewById(R.id.et_log_notes);
        imgCalendarIcon = view.findViewById(R.id.img_log_calendar_icon);
        rgWatered = view.findViewById(R.id.rg_log_watered);
        rgFertilized = view.findViewById(R.id.rg_log_fertilized);
        rgSunlight = view.findViewById(R.id.rg_log_sunlight);
        rgHealth = view.findViewById(R.id.rg_log_health);
        btnSubmitLog = view.findViewById(R.id.btn_submit_log);

        // 📅 Calendar Selector Popup Logic
        View.OnClickListener dateListener = v -> showDatePickerDialog();
        etLogDate.setOnClickListener(dateListener);
        imgCalendarIcon.setOnClickListener(dateListener);

        // 💾 Submit Log Trigger Button
        btnSubmitLog.setOnClickListener(v -> {
            saveLogToFirebase();
        });

        return view;
    }

    private void saveLogToFirebase() {
        String date = etLogDate.getText().toString().trim();
        String notes = etLogNotes.getText().toString().trim();

        if (date.isEmpty()) {
            Toast.makeText(getContext(), "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean watered = false;
        int wateredId = rgWatered.getCheckedRadioButtonId();
        if (wateredId == R.id.rb_water_yes) watered = true;

        boolean fertilized = false;
        int fertilizedId = rgFertilized.getCheckedRadioButtonId();
        if (fertilizedId == R.id.rb_fertilizer_yes) fertilized = true;

        String sunlight = "";
        int sunlightId = rgSunlight.getCheckedRadioButtonId();
        if (sunlightId != -1) {
            sunlight = ((RadioButton) getView().findViewById(sunlightId)).getText().toString();
        }

        String health = "";
        int healthPercentage = 100; // Default
        
        int healthId = rgHealth.getCheckedRadioButtonId();
        if (healthId != -1) {
            RadioButton rb = getView().findViewById(healthId);
            if (rb != null) {
                health = rb.getText().toString();
                
                // 📊 Kalkulahin ang percentage base sa status string
                if (health.equalsIgnoreCase("Healthy")) healthPercentage = 100;
                else if (health.equalsIgnoreCase("Fair")) healthPercentage = 75;
                else if (health.equalsIgnoreCase("Needs Attention")) healthPercentage = 50;
                else if (health.equalsIgnoreCase("Critical")) healthPercentage = 25;
            }
        }

        if (databaseReference == null) {
            Toast.makeText(getContext(), "Error: Database reference not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String logId = databaseReference.push().getKey();
        if (logId == null) return;
        
        LogModel log = new LogModel(logId, date, watered, fertilized, sunlight, health, notes);

        final String finalHealth = health;
        final int finalPercentage = healthPercentage;
        
        databaseReference.child(logId).setValue(log).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 🚀 I-update ang main plant record sa Firebase (parehong Status at Percentage)
                DatabaseReference plantRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(currentUsername).child("plants").child(plantId);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("healthStatus", finalHealth);
                updates.put("healthPercentage", finalPercentage);
                
                plantRef.updateChildren(updates);

                Toast.makeText(getContext(), "New Plant Growth Log Saved!", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to save log.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etLogDate.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }
}