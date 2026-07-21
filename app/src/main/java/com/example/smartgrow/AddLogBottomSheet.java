package com.example.smartgrow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    private SharedPrefManager prefManager;

    private View rootView;

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

        // 🔐 SECURE DATA FETCH
        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown") && plantId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId).child("logs");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setHideable(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                int displayHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
                behavior.setMaxHeight((int) (displayHeight * 0.90));
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.dialog_add_log_sheet, container, false);

        etLogDate = rootView.findViewById(R.id.et_log_date);
        etLogNotes = rootView.findViewById(R.id.et_log_notes);
        imgCalendarIcon = rootView.findViewById(R.id.img_log_calendar_icon);
        rgWatered = rootView.findViewById(R.id.rg_log_watered);
        rgFertilized = rootView.findViewById(R.id.rg_log_fertilized);
        rgSunlight = rootView.findViewById(R.id.rg_log_sunlight);
        rgHealth = rootView.findViewById(R.id.rg_log_health);
        btnSubmitLog = rootView.findViewById(R.id.btn_submit_log);

        View.OnClickListener dateListener = v -> showDatePickerDialog();
        etLogDate.setOnClickListener(dateListener);
        imgCalendarIcon.setOnClickListener(dateListener);

        btnSubmitLog.setOnClickListener(v -> saveLogToFirebase());

        return rootView;
    }

    private void saveLogToFirebase() {
        if (databaseReference == null) {
            Toast.makeText(getContext(), "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        if (sunlightId != -1 && rootView != null) {
            RadioButton rb = rootView.findViewById(sunlightId);
            if (rb != null) {
                sunlight = rb.getText().toString();
            }
        }

        String health = "";
        int healthPercentage = 100;

        int healthId = rgHealth.getCheckedRadioButtonId();
        if (healthId != -1 && rootView != null) {
            RadioButton rb = rootView.findViewById(healthId);
            if (rb != null) {
                health = rb.getText().toString();

                if (health.equalsIgnoreCase("Healthy")) healthPercentage = 100;
                else if (health.equalsIgnoreCase("Fair")) healthPercentage = 75;
                else if (health.equalsIgnoreCase("Needs Attention")) healthPercentage = 50;
                else if (health.equalsIgnoreCase("Critical")) healthPercentage = 25;
            }
        }

        String logId = databaseReference.push().getKey();
        if (logId == null) return;

        LogModel log = new LogModel(logId, date, watered, fertilized, sunlight, health, notes);

        final String finalHealth = health;
        final int finalPercentage = healthPercentage;

        databaseReference.child(logId).setValue(log).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference plantRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(currentUsername).child("plants").child(plantId);

                Map<String, Object> updates = new HashMap<>();
                updates.put("healthStatus", finalHealth);
                updates.put("healthPercentage", finalPercentage);

                plantRef.updateChildren(updates);

                Toast.makeText(getContext(), "Log Saved!", Toast.LENGTH_SHORT).show();
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
