package com.example.smartgrow;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.database.FirebaseDatabase;

public class SmartGrowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        /* 
           🚀 AVAILABILITY (CIA Triad):
           Ine-enable nito ang offline persistence. 
           Kahit walang internet, mababasa pa rin ng user ang data.
           Awtomatikong mag-sy-sync ang data kapag bumalik ang connection.
        */
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // 🔔 Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this);
    }
}
