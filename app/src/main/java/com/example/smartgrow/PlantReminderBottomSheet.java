package com.example.smartgrow;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.Locale;

public class PlantReminderBottomSheet extends BottomSheetDialogFragment {

    private Spinner spinnerWater, spinnerFertilizer, spinnerSunlight; // 🌟 Idinagdag si spinnerSunlight
    private EditText etTime;
    private MaterialButton btnSave;

    private String plantId;
    private DatabaseReference databaseReference;
    private String currentUsername;

    public static PlantReminderBottomSheet newInstance(String plantId) {
        PlantReminderBottomSheet fragment = new PlantReminderBottomSheet();
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
                    .child(currentUsername).child("plants").child(plantId).child("reminders");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_plant_reminder_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔗 Bind View Layout Components
        spinnerWater = view.findViewById(R.id.spinner_watering_frequency);
        spinnerFertilizer = view.findViewById(R.id.spinner_fertilizing_frequency);
        spinnerSunlight = view.findViewById(R.id.spinner_sunlight_frequency); // 🔗 Ikonek si sunlight
        etTime = view.findViewById(R.id.et_reminder_time);
        btnSave = view.findViewById(R.id.btn_save_reminder);

        // 🛠️ Construct Dropdown Options
        String[] waterOptions = {"Every Day", "Every 2 Days", "Every 3 Days", "Weekly", "None"};
        String[] fertilizerOptions = {"Every Week", "Every 2 Weeks", "Monthly", "None"};
        String[] sunlightOptions = {"Every Day", "Every 2 Days", "Weekly", "None"}; // 🌟 Mga pagpipilian para sa araw

        ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, waterOptions);
        ArrayAdapter<String> fertAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, fertilizerOptions);
        ArrayAdapter<String> sunAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sunlightOptions);

        spinnerWater.setAdapter(waterAdapter);
        spinnerFertilizer.setAdapter(fertAdapter);
        spinnerSunlight.setAdapter(sunAdapter); // 🛠️ Isalpak ang adapter kay sunlight

        // ⏰ Hook TimePicker Dialog on Focus/Click
        etTime.setOnClickListener(v -> showTimePicker());

        // 🚀 Save Transaction Logic
        btnSave.setOnClickListener(v -> {
            String waterSched = spinnerWater.getSelectedItem().toString();
            String fertSched = spinnerFertilizer.getSelectedItem().toString();
            String sunSched = spinnerSunlight.getSelectedItem().toString(); // 🌟 Kunin ang string ng sunlight schedule
            String timeSet = etTime.getText().toString().trim();

            if (timeSet.isEmpty()) {
                Toast.makeText(getContext(), "Please set a preferred reminder time!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseReference == null) {
                Toast.makeText(getContext(), "Error: Database reference not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            ReminderModel reminder = new ReminderModel(waterSched, fertSched, sunSched, timeSet);
            databaseReference.setValue(reminder).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Care schedules saved successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to save reminders.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, selectedMinute) -> {
                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                    if (displayHour == 0) displayHour = 12;

                    String formatTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, selectedMinute, amPm);
                    etTime.setText(formatTime);
                }, hour, minute, false);
        timePickerDialog.show();
    }
}