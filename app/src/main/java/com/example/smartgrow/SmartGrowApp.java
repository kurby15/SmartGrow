package com.example.smartgrow;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SmartGrowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
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
