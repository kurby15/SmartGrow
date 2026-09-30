package com.example.smartgrow.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartgrow.plants.MyGardenPlantModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "System Boot Completed. Rescheduling reminders...");
            
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                db.collection("diary")
                        .whereEqualTo("userId", currentUser.getUid())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                MyGardenPlantModel plant = doc.toObject(MyGardenPlantModel.class);
                                if (plant != null && plant.getReminders() != null) {
                                    Map<String, Object> reminders = plant.getReminders();
                                    String time = (String) reminders.get("preferredTime");
                                    
                                    if (time != null && !time.isEmpty()) {
                                        String waterFreq = (String) reminders.get("wateringSchedule");
                                        String fertFreq = (String) reminders.get("fertilizerSchedule");
                                        String sunFreq = (String) reminders.get("sunlightSchedule");

                                        if (waterFreq != null && !"None".equalsIgnoreCase(waterFreq)) {
                                            NotificationHelper.scheduleReminder(context, plant.getId(), plant.getPlantName(), "Water", time, waterFreq);
                                        }
                                        if (fertFreq != null && !"None".equalsIgnoreCase(fertFreq)) {
                                            NotificationHelper.scheduleReminder(context, plant.getId(), plant.getPlantName(), "Fertilize", time, fertFreq);
                                        }
                                        if (sunFreq != null && !"None".equalsIgnoreCase(sunFreq)) {
                                            NotificationHelper.scheduleReminder(context, plant.getId(), plant.getPlantName(), "Sunlight", time, sunFreq);
                                        }
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error rescheduling reminders on boot", e));
            }
        }
    }
}
