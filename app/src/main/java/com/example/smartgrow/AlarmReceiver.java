package com.example.smartgrow;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String plantId = intent.getStringExtra("plantId");
        String plantName = intent.getStringExtra("plantName");
        String taskType = intent.getStringExtra("taskType");
        String timeStr = intent.getStringExtra("timeStr");
        String frequency = intent.getStringExtra("frequency");

        if (plantName == null) plantName = "your plant";

        String title = "SmartGrow Reminder 🌿";
        String message = "It's time to " + taskType.toLowerCase() + " your " + plantName + "! Happy growing! ✨";

        Intent notifyIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (plantName + taskType).hashCode(), 
                notifyIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify((plantName + taskType).hashCode(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // 🚀 RESCHEDULE FOR NEXT TIME (Dahil ang setExactAndAllowWhileIdle ay isang beses lang)
        if (plantId != null && timeStr != null && frequency != null && !frequency.equalsIgnoreCase("None")) {
            NotificationHelper.scheduleReminder(context, plantId, plantName, taskType, timeStr, frequency);
        }
    }
}
