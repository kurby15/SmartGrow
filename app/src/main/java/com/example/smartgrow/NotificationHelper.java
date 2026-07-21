package com.example.smartgrow;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_ID = "SmartGrowReminders";
    public static final String CHANNEL_NAME = "Plant Care Reminders";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for watering, fertilizing, and sunlight.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleReminder(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency) {
        try {
            String[] parts = timeStr.split(" ");
            String[] timeParts = parts[0].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            String amPm = parts[1];

            if (amPm.equalsIgnoreCase("PM") && hour < 12) hour += 12;
            if (amPm.equalsIgnoreCase("AM") && hour == 12) hour = 0;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // If time has already passed today, schedule for the next interval
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }

            long interval = AlarmManager.INTERVAL_DAY; // Default
            if (frequency.equalsIgnoreCase("Every 2 Days")) interval = AlarmManager.INTERVAL_DAY * 2;
            else if (frequency.equalsIgnoreCase("Every 3 Days")) interval = AlarmManager.INTERVAL_DAY * 3;
            else if (frequency.equalsIgnoreCase("Weekly") || frequency.equalsIgnoreCase("Every Week")) interval = AlarmManager.INTERVAL_DAY * 7;
            else if (frequency.equalsIgnoreCase("Every 2 Weeks")) interval = AlarmManager.INTERVAL_DAY * 14;
            else if (frequency.equalsIgnoreCase("Monthly")) interval = AlarmManager.INTERVAL_DAY * 30;

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("plantName", plantName);
            intent.putExtra("taskType", taskType);
            
            int requestCode = (plantId + taskType).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    requestCode, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        interval,
                        pendingIntent
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelReminder(Context context, String plantId, String taskType) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = (plantId + taskType).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }
}
