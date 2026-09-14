package com.example.smartgrow.core;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.smartgrow.MainActivity;
import com.example.smartgrow.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String plantId = intent.getStringExtra("plantId");
        String plantName = intent.getStringExtra("plantName");
        String taskType = intent.getStringExtra("taskType");
        String timeStr = intent.getStringExtra("timeStr");
        String frequency = intent.getStringExtra("frequency");
        String notificationType = intent.getStringExtra("notificationType"); // "BEFORE", "DUE", "OVERDUE"

        if (plantId == null) return;

        // 🔔 Check Firestore to see if the task was already done today before showing notification
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("diary").document(plantId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String lastDoneDate = null;
                
                if ("Water".equals(taskType)) {
                    lastDoneDate = documentSnapshot.getString("lastWateredDate");
                } else if ("Fertilize".equals(taskType)) {
                    lastDoneDate = documentSnapshot.getString("lastFertilizedDate");
                } else if ("Sunlight".equals(taskType) || "Check".equals(taskType)) {
                    lastDoneDate = documentSnapshot.getString("lastCheckedDate");
                }

                // If already done today, don't show "DUE" or "OVERDUE" notifications
                if (todayDate.equals(lastDoneDate) && ("DUE".equals(notificationType) || "OVERDUE".equals(notificationType))) {
                    Log.d(TAG, "Task " + taskType + " for " + plantName + " already done today. Skipping notification.");
                    
                    // Still reschedule for next cycle if this is the DUE trigger
                    if ("DUE".equals(notificationType) && frequency != null && !"None".equalsIgnoreCase(frequency)) {
                        NotificationHelper.scheduleReminder(context, plantId, plantName, taskType, timeStr, frequency, true);
                    }
                    return;
                }
                
                // Proceed with showing notification
                showNotification(context, plantId, plantName, taskType, timeStr, frequency, notificationType);
            }
        }).addOnFailureListener(e -> {
            // Fallback: Proceed if Firestore check fails
            showNotification(context, plantId, plantName, taskType, timeStr, frequency, notificationType);
        });
    }

    private void showNotification(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency, String notificationType) {
        Log.d(TAG, "Notification firing for: " + plantName + " - " + taskType + " (" + notificationType + ")");

        if (plantName == null) plantName = "your plant";

        String title = "SmartGrow Care Reminder 🌿";
        String message;

        if ("BEFORE".equals(notificationType)) {
            message = "Reminder: It will be time to " + (taskType != null ? taskType.toLowerCase() : "care for") + " your " + plantName + " in 2 hours! ✨";
        } else if ("OVERDUE".equals(notificationType)) {
            message = "Action Needed: You haven't " + (taskType != null ? taskType.toLowerCase() + "ed" : "checked") + " your " + plantName + " yet today. Please do it now! ⚠️";
        } else {
            message = "Time to " + (taskType != null ? taskType.toLowerCase() : "care for") + " your " + plantName + "! 💧";
        }

        Intent notifyIntent = new Intent(context, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Consistent request code with NotificationHelper
        int requestCode = (plantId + taskType + notificationType).hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                requestCode, 
                notifyIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder != 0 ? R.drawable.ic_reminder : android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(requestCode, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }

        // Reschedule for next cycle when the DUE alarm fires
        if ("DUE".equals(notificationType) && plantId != null && timeStr != null && frequency != null && !frequency.equalsIgnoreCase("None")) {
            NotificationHelper.scheduleReminder(context, plantId, plantName, taskType, timeStr, frequency, true);
        }
    }
}