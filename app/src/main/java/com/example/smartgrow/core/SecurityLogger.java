package com.example.smartgrow.core;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SecurityLogger {
    private static final String TAG = "SecurityLogger";
    private static final String COLLECTION_NAME = "security_logs";

    public enum SecurityEvent {
        AUTH_SUCCESS,
        AUTH_FAILURE,
        AUTHORIZATION_DENIED,
        INPUT_VALIDATION_FAILURE,
        TAMPERING_DETECTED,
        EXCEPTION_OCCURRED,
        DATA_INTEGRITY_VIOLATION
    }

    public static void log(SecurityEvent event, String detail) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event", event.name());
        logEntry.put("detail", detail);
        logEntry.put("uid", uid);
        logEntry.put("timestamp", System.currentTimeMillis());

        // Local logging
        Log.w(TAG, "Security Event: " + event.name() + " | Detail: " + detail + " | UID: " + uid);

        // Remote logging to Firestore (Audit Trail)
        FirebaseFirestore.getInstance().collection(COLLECTION_NAME)
                .add(logEntry)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send security log to Firestore", e));
    }
}
