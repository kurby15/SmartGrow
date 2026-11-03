package com.example.smartgrow.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;

public class IntegrityManager {

    public static boolean isDeviceCompromised(Context context) {
        return isRooted() || isDebuggable(context);
    }

    private static boolean isRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) return true;
        
        return false;
    }

    private static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static void checkIntegrity(Context context) {
        if (isDeviceCompromised(context)) {
            SecurityLogger.log(SecurityLogger.SecurityEvent.TAMPERING_DETECTED, "Device integrity compromised (Root/Debug)");
            // In a real app, we might want to shut down or restrict features here.
        }
    }
}
