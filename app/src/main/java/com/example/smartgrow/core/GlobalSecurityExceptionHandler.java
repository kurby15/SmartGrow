package com.example.smartgrow.core;

import androidx.annotation.NonNull;
import android.util.Log;

public class GlobalSecurityExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalSecurityExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        // Log the exception as a security event if it might be related to tampering or crashes in security modules
        SecurityLogger.log(SecurityLogger.SecurityEvent.EXCEPTION_OCCURRED, 
            "Uncaught exception in thread " + t.getName() + ": " + e.getMessage());

        // Call the original handler to let the app crash normally
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        }
    }

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalSecurityExceptionHandler());
    }
}
