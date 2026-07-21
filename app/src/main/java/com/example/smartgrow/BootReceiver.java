package com.example.smartgrow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all alarms from Firebase
            SharedPrefManager prefManager = SharedPrefManager.getInstance(context);
            String username = prefManager.getUsername();

            if (!username.equals("unknown")) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(username).child("plants");
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot plantSnap : snapshot.getChildren()) {
                            PlantModel plant = plantSnap.getValue(PlantModel.class);
                            if (plant != null && plant.getReminders() != null) {
                                ReminderModel rem = plant.getReminders();
                                String time = rem.getPreferredTime();
                                if (time != null && !time.isEmpty()) {
                                    if (!"None".equals(rem.getWateringSchedule())) 
                                        NotificationHelper.scheduleReminder(context, plant.getId(), plant.getName(), "Water", time, rem.getWateringSchedule());
                                    if (!"None".equals(rem.getSunlightSchedule())) 
                                        NotificationHelper.scheduleReminder(context, plant.getId(), plant.getName(), "Sunlight", time, rem.getSunlightSchedule());
                                    if (!"None".equals(rem.getFertilizerSchedule())) 
                                        NotificationHelper.scheduleReminder(context, plant.getId(), plant.getName(), "Fertilize", time, rem.getFertilizerSchedule());
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }
    }
}
