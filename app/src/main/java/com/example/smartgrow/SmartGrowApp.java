package com.example.smartgrow;

import android.app.Activity;
import android.app.Application;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SmartGrowApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Ito ang tagapakinig sa lahat ng Activity na bubukas sa app mo
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

                // Kukunin natin ang pinaka-root view ng Activity na kakabukas lang
                View decorView = activity.getWindow().getDecorView();

                // Awtomatikong aalamin kung may navigation bar sa ilalim ng phone
                ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    // Lalagyan natin ng safe padding ang ilalim ng screen
                    // para hindi kainin ng 3-button navigation ang UI mo
                    v.setPadding(
                            v.getPaddingLeft(),
                            v.getPaddingTop(),
                            v.getPaddingRight(),
                            systemBars.bottom // Eto yung magic height ng nav bar ng phone
                    );

                    return insets;
                });
            }

            // Iwanang blanko ang mga ito (Required lang sila ng interface pero wag mo na galawin)
            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}