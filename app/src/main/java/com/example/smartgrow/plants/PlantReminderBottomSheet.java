package com.example.smartgrow.plants;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.smartgrow.R;
import com.example.smartgrow.core.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlantReminderBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "PlantReminderBS";

    private AutoCompleteTextView actvWater, actvFertilizer, actvSunlight;
    private MaterialCardView cardWater, cardFertilizer, cardSunlight;
    private EditText etTime;
    private ImageView imgClockIcon;
    private MaterialButton btnSave;

    private String plantId, plantName = "your plant";
    private String oldPreferredTime = ""; 
    private FirebaseFirestore db;
    private boolean isAutoSchedule = false;

    // Permission launcher for Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                saveScheduleAndNotify();
                if (!isGranted) {
                    Toast.makeText(getContext(), "Notification permission denied. You can enable it in settings to receive reminders.", Toast.LENGTH_LONG).show();
                }
            });

    public static PlantReminderBottomSheet newInstance(String plantId) {
        return newInstance(plantId, false);
    }

    public static PlantReminderBottomSheet newInstance(String plantId, boolean autoSchedule) {
        PlantReminderBottomSheet fragment = new PlantReminderBottomSheet();
        Bundle args = new Bundle();
        args.putString("key_plant_id", plantId);
        args.putBoolean("key_auto_schedule", autoSchedule);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            plantId = getArguments().getString("key_plant_id");
            isAutoSchedule = getArguments().getBoolean("key_auto_schedule", false);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_plant_reminder_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        actvWater = view.findViewById(R.id.actv_watering_frequency);
        actvFertilizer = view.findViewById(R.id.actv_fertilizing_frequency);
        actvSunlight = view.findViewById(R.id.actv_sunlight_frequency);

        cardWater = view.findViewById(R.id.card_watering_frequency);
        cardFertilizer = view.findViewById(R.id.card_fertilizing_frequency);
        cardSunlight = view.findViewById(R.id.card_sunlight_frequency);

        etTime = view.findViewById(R.id.et_reminder_time);
        imgClockIcon = view.findViewById(R.id.img_clock_icon);
        btnSave = view.findViewById(R.id.btn_save_reminder);

        setupAdapters();

        cardWater.setOnClickListener(v -> actvWater.showDropDown());
        actvWater.setOnClickListener(v -> actvWater.showDropDown());
        cardFertilizer.setOnClickListener(v -> actvFertilizer.showDropDown());
        actvFertilizer.setOnClickListener(v -> actvFertilizer.showDropDown());
        cardSunlight.setOnClickListener(v -> actvSunlight.showDropDown());
        actvSunlight.setOnClickListener(v -> actvSunlight.showDropDown());

        View.OnClickListener timePickerListener = v -> showTimePicker();
        etTime.setOnClickListener(timePickerListener);
        imgClockIcon.setOnClickListener(timePickerListener);

        btnSave.setOnClickListener(v -> handleSaveWithPermission());

        fetchExistingData();
    }

    private void setupAdapters() {
        String[] waterOptions = {"Every Day", "Every 2 Days", "Every 3 Days", "Weekly", "None"};
        String[] fertilizerOptions = {"Every Week", "Every 2 Weeks", "Monthly", "None"};
        String[] sunlightOptions = {"Every Day", "Every 2 Days", "Weekly", "None"};

        ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, waterOptions);
        ArrayAdapter<String> fertilizerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, fertilizerOptions);
        ArrayAdapter<String> sunlightAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sunlightOptions);

        actvWater.setAdapter(waterAdapter);
        actvFertilizer.setAdapter(fertilizerAdapter);
        actvSunlight.setAdapter(sunlightAdapter);
    }

    private void fetchExistingData() {
        if (plantId == null) return;
        db.collection("diary").document(plantId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && documentSnapshot.exists()) {
                        String fetchedPlantName = documentSnapshot.getString("plantName");
                        if (fetchedPlantName != null) {
                            plantName = fetchedPlantName;
                        }

                        Map<String, Object> reminders = (Map<String, Object>) documentSnapshot.get("reminders");
                        if (reminders != null) {
                            String water = (String) reminders.get("wateringSchedule");
                            String fert = (String) reminders.get("fertilizerSchedule");
                            String sun = (String) reminders.get("sunlightSchedule");
                            String time = (String) reminders.get("preferredTime");

                            if (water != null) actvWater.setText(water, false);
                            if (fert != null) actvFertilizer.setText(fert, false);
                            if (sun != null) actvSunlight.setText(sun, false);
                            if (time != null) {
                                etTime.setText(time);
                                oldPreferredTime = time;
                            }
                        } else if (isAutoSchedule) {
                            // Automatically set default reminders for sick plant (< 50% health)
                            actvWater.setText("Every Day", false);
                            actvFertilizer.setText("Every Week", false);
                            actvSunlight.setText("Every Day", false);
                            etTime.setText("08:00 AM");
                            
                            // Automatically save the schedule as requested
                            handleSaveWithPermission();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching existing data", e));
    }

    private void handleSaveWithPermission() {
        if (isInputInvalid()) {
            Toast.makeText(getContext(), "Please fill in all care details!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                saveScheduleAndNotify();
            }
        } else {
            saveScheduleAndNotify();
        }
    }

    private boolean isInputInvalid() {
        return actvWater.getText().toString().trim().isEmpty() ||
                actvFertilizer.getText().toString().trim().isEmpty() ||
                actvSunlight.getText().toString().trim().isEmpty() ||
                etTime.getText().toString().trim().isEmpty();
    }

    private void saveScheduleAndNotify() {
        String waterSched = actvWater.getText().toString().trim();
        String fertSched = actvFertilizer.getText().toString().trim();
        String sunSched = actvSunlight.getText().toString().trim();
        String timeSet = etTime.getText().toString().trim();

        Map<String, Object> reminderData = new HashMap<>();
        reminderData.put("wateringSchedule", waterSched);
        reminderData.put("fertilizerSchedule", fertSched);
        reminderData.put("sunlightSchedule", sunSched);
        reminderData.put("preferredTime", timeSet);

        Map<String, Object> updates = new HashMap<>();
        updates.put("reminders", reminderData);

        if (!timeSet.equals(oldPreferredTime)) {
            updates.put("lastWateredDate", "");
            updates.put("lastFertilizedDate", "");
            updates.put("lastCheckedDate", "");
        }

        db.collection("diary").document(plantId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    NotificationHelper.createNotificationChannel(requireContext());

                    if (!waterSched.equals("None"))
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Water", timeSet, waterSched);
                    else
                        NotificationHelper.cancelReminder(requireContext(), plantId, "Water");

                    if (!sunSched.equals("None"))
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Sunlight", timeSet, sunSched);
                    else
                        NotificationHelper.cancelReminder(requireContext(), plantId, "Sunlight");

                    if (!fertSched.equals("None"))
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Fertilize", timeSet, fertSched);
                    else
                        NotificationHelper.cancelReminder(requireContext(), plantId, "Fertilize");

                    Toast.makeText(getContext(), "Schedule saved for " + plantName + "! 🌿", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        new TimePickerDialog(getContext(), (view, hourOfDay, selectedMinute) -> {
            String amPm = (hourOfDay >= 12) ? "PM" : "AM";
            int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
            etTime.setText(String.format(Locale.US, "%02d:%02d %s", displayHour, selectedMinute, amPm));
        }, hour, minute, false).show();
    }
}
