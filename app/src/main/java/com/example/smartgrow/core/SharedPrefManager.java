package com.example.smartgrow.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.example.smartgrow.profile.User;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedPrefManager {
    private static final String PREF_NAME = "SmartGrowSecurePrefs";
    private static final String OLD_PREF_NAME = "SmartGrowPrefs";
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;

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
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        if (user.getUsername() != null && !user.getUsername().isEmpty() && !user.getUsername().equals("unknown")) {
            editor.putString("username", user.getUsername());
        }
        
        if (user.getFullName() != null) editor.putString("fullname", user.getFullName());
        if (user.getProfilePic() != null) editor.putString("profile_pic", user.getProfilePic());
        
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }

    public void saveProfilePic(String profilePic) {
        sharedPreferences.edit().putString("profile_pic", profilePic).apply();
    }

    public String getUsername() {
        return sharedPreferences.getString("username", "unknown");
    }

    public String getFullName() {
        return sharedPreferences.getString("fullname", "SmartGrow User");
    }

    public String getProfilePic() {
        return sharedPreferences.getString("profile_pic", null);
    }

    public boolean isLoggedIn() {
        String username = getUsername();
        return sharedPreferences.getBoolean("is_logged_in", false) && !username.equals("unknown");
    }

    public void logout(Context context) {
        sharedPreferences.edit().clear().apply();
        context.getSharedPreferences(OLD_PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
