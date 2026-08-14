package com.example.smartgrow.utils;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FirebaseCryptoUtils {

    // Modern, secure AEAD mode
    private static final String ALGORITHM_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits standard for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    // Legacy fallback for backward compatibility with previous ECB encryption
    private static final String ALGORITHM_ECB_LEGACY = "AES/ECB/PKCS5Padding";

    /**
     * Converts the user's UID into a secure 256-bit AES key.
     */
    private static SecretKeySpec getKeyFromUid(String uid) throws Exception {
        byte[] key = uid.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a dynamic IV.
     */
    public static String encrypt(String plainText, String uid) {
        if (plainText == null || plainText.isEmpty() || uid == null || uid.isEmpty()) {
            return plainText;
        }
        try {
            // 1. Generate a random 12-byte Initialization Vector (IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. Initialize GCM Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromUid(uid), spec);

            // 3. Encrypt
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. Combine [IV + Ciphertext] into a single byte array
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return plainText; // Fallback to raw text if encryption fails
        }
    }

    /**
     * Decrypts ciphertext. Automatically handles AES-GCM and falls back to legacy AES-ECB.
     */
    public static String decrypt(String cipherText, String uid) {
        if (cipherText == null || cipherText.isEmpty() || uid == null || uid.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP);

            // Try AES-GCM Decryption
            if (decodedBytes.length > GCM_IV_LENGTH) {
                try {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);

                    // Extract IV
                    byte[] iv = new byte[GCM_IV_LENGTH];
                    byteBuffer.get(iv);

                    // Extract Payload
                    byte[] encryptedBytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(encryptedBytes);

                    Cipher cipher = Cipher.getInstance(ALGORITHM_GCM);
                    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                    cipher.init(Cipher.DECRYPT_MODE, getKeyFromUid(uid), spec);

                    byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                    return new String(decryptedBytes, StandardCharsets.UTF_8);
                } catch (Exception gcmException) {
                    // Fallthrough to legacy ECB check if GCM fails
                }
            }

            // Fallback: Legacy ECB Decryption
            return decryptLegacyECB(cipherText, uid);

        } catch (Exception e) {
            e.printStackTrace();
            return cipherText; // Return original if decryption fails (e.g. unencrypted plain text)
        }
    }

    /**
     * Legacy ECB decryption method for data written before GCM migration.
     */
    private static String decryptLegacyECB(String cipherText, String uid) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_ECB_LEGACY);
            cipher.init(Cipher.DECRYPT_MODE, getKeyFromUid(uid));
            byte[] decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText; // Return raw text if not encrypted
        }
    }
}