package com.example.smartgrow.plants;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.smartgrow.R;
import com.example.smartgrow.core.NotificationHelper;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;

public class PlantReminderBottomSheet extends BottomSheetDialogFragment {

    private AutoCompleteTextView actvWater, actvFertilizer, actvSunlight;
    private MaterialCardView cardWater, cardFertilizer, cardSunlight;
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

        // Bind Views base sa mga bagong ID sa XML
        actvWater = view.findViewById(R.id.actv_watering_frequency);
        actvFertilizer = view.findViewById(R.id.actv_fertilizing_frequency);
        actvSunlight = view.findViewById(R.id.actv_sunlight_frequency);

        cardWater = view.findViewById(R.id.card_watering_frequency);
        cardFertilizer = view.findViewById(R.id.card_fertilizing_frequency);
        cardSunlight = view.findViewById(R.id.card_sunlight_frequency);

        etTime = view.findViewById(R.id.et_reminder_time);
        imgClockIcon = view.findViewById(R.id.img_clock_icon);
        btnSave = view.findViewById(R.id.btn_save_reminder);

        String[] waterOptions = {"Every Day", "Every 2 Days", "Every 3 Days", "Weekly", "None"};
        String[] fertilizerOptions = {"Every Week", "Every 2 Weeks", "Monthly", "None"};
        String[] sunlightOptions = {"Every Day", "Every 2 Days", "Weekly", "None"};

        ArrayAdapter<String> waterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, waterOptions);
        ArrayAdapter<String> fertilizerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, fertilizerOptions);
        ArrayAdapter<String> sunlightAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sunlightOptions);

        actvWater.setAdapter(waterAdapter);
        actvFertilizer.setAdapter(fertilizerAdapter);
        actvSunlight.setAdapter(sunlightAdapter);

        // Para kusang lumabas ang dropdown list kapag pinindot ang card o ang field mismo
        cardWater.setOnClickListener(v -> actvWater.showDropDown());
        actvWater.setOnClickListener(v -> actvWater.showDropDown());

        cardFertilizer.setOnClickListener(v -> actvFertilizer.showDropDown());
        actvFertilizer.setOnClickListener(v -> actvFertilizer.showDropDown());

        cardSunlight.setOnClickListener(v -> actvSunlight.showDropDown());
        actvSunlight.setOnClickListener(v -> actvSunlight.showDropDown());

        // Time Picker Listeners
        View.OnClickListener timePickerListener = v -> showTimePicker();
        etTime.setOnClickListener(timePickerListener);
        imgClockIcon.setOnClickListener(timePickerListener);

        // Save Button Action
        btnSave.setOnClickListener(v -> {
            String waterSched = actvWater.getText().toString().trim();
            String fertSched = actvFertilizer.getText().toString().trim();
            String sunSched = actvSunlight.getText().toString().trim();
            String timeSet = etTime.getText().toString().trim();

            if (waterSched.isEmpty() || fertSched.isEmpty() || sunSched.isEmpty()) {
                Toast.makeText(getContext(), "Please select all care schedules!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (timeSet.isEmpty()) {
                Toast.makeText(getContext(), "Please set a preferred reminder time!", Toast.LENGTH_SHORT).show();
                return;
            }

            ReminderModel reminder = new ReminderModel(waterSched, fertSched, sunSched, timeSet);
            databaseReference.setValue(reminder).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
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