package com.example.smartgrow.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.example.smartgrow.profile.User;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

public class SharedPrefManager {
    private static final String PREF_NAME = "SmartGrowSecurePrefs";
    private static final String OLD_PREF_NAME = "SmartGrowPrefs";
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;

    private static final String KEY_SCAN_COUNT = "scan_usage_count";
    private static final String KEY_SCAN_TIME = "scan_first_usage_time";
    private static final String KEY_CHAT_COUNT = "chat_usage_count";
    private static final String KEY_CHAT_TIME = "chat_first_usage_time";
    public static final int MAX_PROMPTS = 15;
    private static final long RESET_INTERVAL = TimeUnit.HOURS.toMillis(12);

    private SharedPrefManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveUser(User user) {
        if (sharedPreferences == null || user == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (user.getUid() != null) editor.putString("uid", user.getUid());
        if (user.getUsername() != null && !user.getUsername().isEmpty() && !user.getUsername().equals("unknown")) {
            editor.putString("username", user.getUsername());
        }

        if (user.getFullName() != null) editor.putString("fullname", user.getFullName());
        if (user.getEmail() != null) editor.putString("email", user.getEmail());
        if (user.getAddress() != null) editor.putString("address", user.getAddress());
        if (user.getPhone() != null) editor.putString("phone", user.getPhone());
        if (user.getProfilePic() != null) editor.putString("profile_pic", user.getProfilePic());

        editor.putBoolean("is_logged_in", true);
        editor.commit();
    }

    public User getUser() {
        if (!isLoggedIn()) return null;
        User user = new User();
        user.setUid(sharedPreferences.getString("uid", null));
        user.setUsername(getUsername());
        user.setFullName(getFullName());
        user.setEmail(sharedPreferences.getString("email", null));
        user.setAddress(sharedPreferences.getString("address", null));
        user.setPhone(sharedPreferences.getString("phone", null));
        user.setProfilePic(getProfilePic());
        return user;
    }

    public void saveProfilePic(String profilePic) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString("profile_pic", profilePic).commit();
        }
    }

    public String getUsername() {
        return sharedPreferences != null ? sharedPreferences.getString("username", "unknown") : "unknown";
    }

    public String getFullName() {
        return sharedPreferences != null ? sharedPreferences.getString("fullname", "SmartGrow User") : "SmartGrow User";
    }

    public String getProfilePic() {
        return sharedPreferences != null ? sharedPreferences.getString("profile_pic", null) : null;
    }

    public boolean isLoggedIn() {
        if (sharedPreferences == null) return false;
        String username = getUsername();
        return sharedPreferences.getBoolean("is_logged_in", false) && !username.equals("unknown");
    }

    public boolean isFirstTimeScan() {
        return sharedPreferences != null && sharedPreferences.getBoolean("first_time_scan", true);
    }

    public void setFirstTimeScan(boolean isFirstTime) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean("first_time_scan", isFirstTime).apply();
        }
    }

    public boolean canUseScan() {
        return checkAndIncrementLimit(KEY_SCAN_COUNT, KEY_SCAN_TIME);
    }

    public boolean canUseChat() {
        return checkAndIncrementLimit(KEY_CHAT_COUNT, KEY_CHAT_TIME);
    }

    public long getScanTimeRemaining() {
        return getTimeRemaining(KEY_SCAN_TIME);
    }

    public long getChatTimeRemaining() {
        return getTimeRemaining(KEY_CHAT_TIME);
    }

    private boolean checkAndIncrementLimit(String countKey, String timeKey) {
        if (sharedPreferences == null) return true;
        
        long currentTime = System.currentTimeMillis();
        long firstUsageTime = sharedPreferences.getLong(timeKey, 0);
        int currentCount = sharedPreferences.getInt(countKey, 0);

        if (firstUsageTime == 0 || (currentTime - firstUsageTime) > RESET_INTERVAL) {
            // Reset window
            sharedPreferences.edit()
                    .putLong(timeKey, currentTime)
                    .putInt(countKey, 1)
                    .apply();
            return true;
        }

        if (currentCount < MAX_PROMPTS) {
            sharedPreferences.edit()
                    .putInt(countKey, currentCount + 1)
                    .apply();
            return true;
        }

        return false;
    }

    private long getTimeRemaining(String timeKey) {
        if (sharedPreferences == null) return 0;
        long firstUsageTime = sharedPreferences.getLong(timeKey, 0);
        if (firstUsageTime == 0) return 0;
        
        long diff = System.currentTimeMillis() - firstUsageTime;
        if (diff > RESET_INTERVAL) return 0;
        
        return RESET_INTERVAL - diff;
    }

    public void logout(Context context) {
        try {
            if (sharedPreferences != null) {
                sharedPreferences.edit().clear().commit();
            }
            if (context != null) {
                context.getSharedPreferences(OLD_PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            instance = null;
        }
    }
}
