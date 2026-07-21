package com.example.smartgrow;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;

public class PlantReminderBottomSheet extends BottomSheetDialogFragment {

    private Spinner spinnerWater, spinnerFertilizer, spinnerSunlight;
    private EditText etTime;
    private ImageView imgClockIcon;
    private MaterialButton btnSave;

    private String plantId, plantName = "your plant";
    private DatabaseReference databaseReference, plantNameRef;
    private String currentUsername;
    private SharedPrefManager prefManager;

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

        prefManager = SharedPrefManager.getInstance(requireContext());
        currentUsername = prefManager.getUsername();

        if (currentUsername != null && !currentUsername.isEmpty() && !currentUsername.equals("unknown") && plantId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId).child("reminders");
            
            // Get plant name for notification
            plantNameRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUsername).child("plants").child(plantId).child("name");
            
            plantNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) plantName = snapshot.getValue(String.class);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
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

        spinnerWater = view.findViewById(R.id.spinner_watering_frequency);
        spinnerFertilizer = view.findViewById(R.id.spinner_fertilizing_frequency);
        spinnerSunlight = view.findViewById(R.id.spinner_sunlight_frequency);
        etTime = view.findViewById(R.id.et_reminder_time);
        imgClockIcon = view.findViewById(R.id.img_clock_icon);
        btnSave = view.findViewById(R.id.btn_save_reminder);

        String[] waterOptions = {"Every Day", "Every 2 Days", "Every 3 Days", "Weekly", "None"};
        String[] fertilizerOptions = {"Every Week", "Every 2 Weeks", "Monthly", "None"};
        String[] sunlightOptions = {"Every Day", "Every 2 Days", "Weekly", "None"};

        spinnerWater.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, waterOptions));
        spinnerFertilizer.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, fertilizerOptions));
        spinnerSunlight.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sunlightOptions));

        View.OnClickListener timePickerListener = v -> showTimePicker();
        etTime.setOnClickListener(timePickerListener);
        imgClockIcon.setOnClickListener(timePickerListener);

        btnSave.setOnClickListener(v -> {
            String waterSched = spinnerWater.getSelectedItem().toString();
            String fertSched = spinnerFertilizer.getSelectedItem().toString();
            String sunSched = spinnerSunlight.getSelectedItem().toString();
            String timeSet = etTime.getText().toString().trim();

            if (timeSet.isEmpty()) {
                Toast.makeText(getContext(), "Please set a preferred reminder time!", Toast.LENGTH_SHORT).show();
                return;
            }

            ReminderModel reminder = new ReminderModel(waterSched, fertSched, sunSched, timeSet);
            databaseReference.setValue(reminder).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 🚀 SCHEDULE THE REAL ALARM NOTIFICATION
                    NotificationHelper.createNotificationChannel(requireContext());
                    
                    if (!waterSched.equals("None")) 
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Water", timeSet, waterSched);
                    if (!sunSched.equals("None")) 
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Sunlight", timeSet, sunSched);
                    if (!fertSched.equals("None")) 
                        NotificationHelper.scheduleReminder(requireContext(), plantId, plantName, "Fertilize", timeSet, fertSched);

                    Toast.makeText(getContext(), "Schedule saved! I will remind you at " + timeSet + "! 🌿", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        });
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        new TimePickerDialog(getContext(), (view, hourOfDay, selectedMinute) -> {
            String amPm = (hourOfDay >= 12) ? "PM" : "AM";
            int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, selectedMinute, amPm));
        }, hour, minute, false).show();
    }
}
