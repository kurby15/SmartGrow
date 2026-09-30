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