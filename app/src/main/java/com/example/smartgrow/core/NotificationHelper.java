package com.example.smartgrow.core;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_ID = "care_reminders";
    public static final String CHANNEL_NAME = "Care Reminders";
    private static final String TAG = "NotificationHelper";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled plant care reminders.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleReminder(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency) {
        scheduleReminder(context, plantId, plantName, taskType, timeStr, frequency, false);
    }

    public static void scheduleReminder(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency, boolean isReschedule) {
        try {
            if (!isReschedule) {
                cancelReminder(context, plantId, taskType);
                Log.d(TAG, "Care reminder saved: ID=" + plantId + ", taskType=" + taskType + ", time=" + timeStr);
            }

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
            calendar.set(Calendar.MILLISECOND, 0);

            if (isReschedule) {
                int daysToAdd = getDaysFromFrequency(frequency);
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
            } else {
                // If the scheduled time for TODAY has already passed, schedule for tomorrow
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            long triggerTime = calendar.getTimeInMillis();

            // 1. Schedule "DUE" Alarm (Exactly at preferred time)
            setAlarm(context, plantId, plantName, taskType, timeStr, frequency, triggerTime, "DUE");

            // 2. Schedule "BEFORE" Alarm (2 hours before)
            Calendar beforeCal = (Calendar) calendar.clone();
            beforeCal.add(Calendar.HOUR_OF_DAY, -2);
            // CHANGED: Removed the 'if' check so past "BEFORE" alarms for today don't get completely dropped
            setAlarm(context, plantId, plantName, taskType, timeStr, frequency, beforeCal.getTimeInMillis(), "BEFORE");

            // 3. Schedule "OVERDUE" Alarm (2 hours after)
            Calendar afterCal = (Calendar) calendar.clone();
            afterCal.add(Calendar.HOUR_OF_DAY, 2);
            setAlarm(context, plantId, plantName, taskType, timeStr, frequency, afterCal.getTimeInMillis(), "OVERDUE");

            Log.d(TAG, "Care reminder alarm scheduled: ID=" + plantId + ", taskType=" + taskType + ", triggerTime=" + calendar.getTime().toString());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder", e);
        }
    }

    private static void setAlarm(Context context, String plantId, String plantName, String taskType, String timeStr, String frequency, long triggerTime, String type) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("plantId", plantId);
        intent.putExtra("plantName", plantName);
        intent.putExtra("taskType", taskType);
        intent.putExtra("timeStr", timeStr);
        intent.putExtra("frequency", frequency);
        intent.putExtra("notificationType", type);

        // Fixed unique request code per plant/task/type combination
        int requestCode = (plantId + taskType + type).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // CHANGED: Fixed conditional logic so exact alarms are always properly scheduled regardless of SDK version or extra permission prompt locks
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private static int getDaysFromFrequency(String frequency) {
        if (frequency == null) return 1;
        switch (frequency) {
            case "Every Day": return 1;
            case "Every 2 Days": return 2;
            case "Every 3 Days": return 3;
            case "Weekly":
            case "Every Week": return 7;
            case "Every 2 Weeks": return 14;
            case "Monthly": return 30;
            default: return 1;
        }
    }

    public static void cancelReminder(Context context, String plantId, String taskType) {
        cancelAlarm(context, plantId, taskType, "DUE");
        cancelAlarm(context, plantId, taskType, "BEFORE");
        cancelAlarm(context, plantId, taskType, "OVERDUE");
        Log.d(TAG, "Care reminder alarm cancelled: ID=" + plantId + ", taskType=" + taskType);
    }

    public static void cancelOverdueReminder(Context context, String plantId, String taskType) {
        cancelAlarm(context, plantId, taskType, "OVERDUE");
    }

    private static void cancelAlarm(Context context, String plantId, String taskType, String type) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = (plantId + taskType + type).hashCode();
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