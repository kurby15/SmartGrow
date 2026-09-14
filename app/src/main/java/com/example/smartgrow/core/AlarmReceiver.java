package com.example.smartgrow.core;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

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
            } else {
                // CHANGED: Added an 'else' block so notifications still show up even if the diary document doesn't exist yet
                showNotification(context, plantId, plantName, taskType, timeStr, frequency, notificationType);
            }
        }).addOnFailureListener(e -> {
            // Fallback: Proceed if Firestore check fails
            showNotification(context, plantId, plantName, taskType, timeStr, frequency, notificationType);
        });
    }

    private void showNotification(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency, String notificationType) {
        Log.d(TAG, "Care reminder notification triggered: ID=" + plantId + ", taskType=" + taskType + " (" + notificationType + ")");

        if (plantName == null) plantName = "your plant";

        // Ensure channel is created immediately before showing notification
        NotificationHelper.createNotificationChannel(context);

        String title = "AirSense Care Reminder";
        String message;

        String actionStr = "check";
        if ("Water".equalsIgnoreCase(taskType)) {
            actionStr = "water";
        } else if ("Fertilize".equalsIgnoreCase(taskType)) {
            actionStr = "fertilize";
        } else if ("Sunlight".equalsIgnoreCase(taskType)) {
            actionStr = "check sunlight for";
        }

        if ("BEFORE".equals(notificationType)) {
            message = "Reminder: It will be time to " + actionStr + " your " + plantName + " soon.";
        } else if ("OVERDUE".equals(notificationType)) {
            message = "Action Needed: Time to " + actionStr + " your " + plantName + " and complete your scheduled care activity.";
        } else {
            message = "Time to " + actionStr + " your " + plantName + ".";
        }

        Intent notifyIntent = new Intent(context, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notifyIntent.putExtra("plantId", plantId);

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

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(requestCode, builder.build());
        }

        // Reschedule for next cycle when the DUE alarm fires
        if ("DUE".equals(notificationType) && plantId != null && timeStr != null && frequency != null && !frequency.equalsIgnoreCase("None")) {
            NotificationHelper.scheduleReminder(context, plantId, plantName, taskType, timeStr, frequency, true);
        }
    }
}