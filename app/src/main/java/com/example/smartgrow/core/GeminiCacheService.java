package com.example.smartgrow.core;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class GeminiCacheService {
    private static final String TAG = "GeminiCacheService";
    private static final String COLLECTION_AI_CACHE = "ai_cache";
    private static final String DOC_PLANTS = "plants";
    private static final String DOC_CHATBOT = "chatbot";
    private static final String DOC_IDENTIFICATION = "identification";

    public static final double PLANT_IDENTIFICATION_THRESHOLD = 0.80;

    private final FirebaseFirestore db;
    private static GeminiCacheService instance;

    // Memory cache for extreme speed within the same session
    private final Map<String, DocumentSnapshot> plantMemoryCache = new HashMap<>();
    private final Map<String, DocumentSnapshot> chatbotMemoryCache = new HashMap<>();
    private final Map<String, String> identificationMemoryCache = new HashMap<>();

    private GeminiCacheService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized GeminiCacheService getInstance() {
        if (instance == null) {
            instance = new GeminiCacheService();
        }
        return instance;
    }

    public String normalizeSpecies(String species) {
        if (species == null || species.trim().isEmpty()) return "unknown_species";
        String cleaned = species.contains("(") ? species.substring(0, species.indexOf("(")).trim() : species;
        return cleaned.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "_");
    }

    public String normalizeQuestion(String question) {
        if (question == null) return "";
        return question.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ");
    }

    // --- Identification Cache ---

    public String getIdentificationFromMemory(String hash) {
        return identificationMemoryCache.get(hash);
    }

    public Task<DocumentSnapshot> getCachedIdentification(String imageHash) {
        if (identificationMemoryCache.containsKey(imageHash)) {
            return Tasks.forResult(null); 
        }
        // Speed Optimization: Check local Source.CACHE first
        return db.collection(COLLECTION_AI_CACHE).document(DOC_IDENTIFICATION).collection("items").document(imageHash).get(Source.CACHE)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String species = task.getResult().getString("speciesName");
                        if (species != null) identificationMemoryCache.put(imageHash, species);
                        return task;
                    }
                    return db.collection(COLLECTION_AI_CACHE).document(DOC_IDENTIFICATION).collection("items").document(imageHash).get(Source.DEFAULT)
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String species = doc.getString("speciesName");
                                    if (species != null) identificationMemoryCache.put(imageHash, species);
                                }
                            });
                });
    }

    public void saveIdentificationCache(String imageHash, String speciesName) {
        identificationMemoryCache.put(imageHash, speciesName);
        Map<String, Object> data = new HashMap<>();
        data.put("speciesName", speciesName);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_AI_CACHE).document(DOC_IDENTIFICATION).collection("items").document(imageHash).set(data, SetOptions.merge());
    }

    // --- Plant Profile Cache ---

    public DocumentSnapshot getPlantFromMemory(String normalizedSpecies) {
        return plantMemoryCache.get(normalizedSpecies);
    }

    public Task<DocumentSnapshot> getCachedPlant(String normalizedSpecies) {
        if (plantMemoryCache.containsKey(normalizedSpecies)) {
            return Tasks.forResult(plantMemoryCache.get(normalizedSpecies));
        }
        // Check local cache first for instant response
        return db.collection(COLLECTION_AI_CACHE).document(DOC_PLANTS).collection("items").document(normalizedSpecies).get(Source.CACHE)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        plantMemoryCache.put(normalizedSpecies, task.getResult());
                        return task;
                    }
                    return db.collection(COLLECTION_AI_CACHE).document(DOC_PLANTS).collection("items").document(normalizedSpecies).get(Source.DEFAULT)
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) plantMemoryCache.put(normalizedSpecies, doc);
                            });
                });
    }

    public void savePlantCache(String normalizedSpecies, String speciesName, String rawJson, Map<String, Object> details) {
        Map<String, Object> cacheData = new HashMap<>();
        cacheData.put("speciesName", speciesName);
        cacheData.put("normalizedSpecies", normalizedSpecies);
        cacheData.put("rawAnalysisJson", rawJson);
        if (details != null) cacheData.putAll(details);
        cacheData.put("aiGenerated", true);
        cacheData.put("source", "Gemini");
        cacheData.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_AI_CACHE).document(DOC_PLANTS).collection("items").document(normalizedSpecies).set(cacheData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    db.collection(COLLECTION_AI_CACHE).document(DOC_PLANTS).collection("items").document(normalizedSpecies).get(Source.CACHE)
                            .addOnSuccessListener(doc -> plantMemoryCache.put(normalizedSpecies, doc));
                });
    }

    // --- Chatbot Response Cache ---

    public DocumentSnapshot getChatResponseFromMemory(String normalizedSpecies, String normalizedQuestion) {
        String cacheKey = (normalizedSpecies != null ? normalizedSpecies : "general") + "__" + normalizedQuestion.replaceAll(" ", "_");
        return chatbotMemoryCache.get(cacheKey);
    }

    public Task<DocumentSnapshot> getCachedChatResponse(String normalizedSpecies, String normalizedQuestion) {
        String cacheKey = (normalizedSpecies != null ? normalizedSpecies : "general") + "__" + normalizedQuestion.replaceAll(" ", "_");
        if (chatbotMemoryCache.containsKey(cacheKey)) {
            return Tasks.forResult(chatbotMemoryCache.get(cacheKey));
        }
        return db.collection(COLLECTION_AI_CACHE).document(DOC_CHATBOT).collection("items").document(cacheKey).get(Source.CACHE)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        chatbotMemoryCache.put(cacheKey, task.getResult());
                        return task;
                    }
                    return db.collection(COLLECTION_AI_CACHE).document(DOC_CHATBOT).collection("items").document(cacheKey).get(Source.DEFAULT)
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) chatbotMemoryCache.put(cacheKey, doc);
                            });
                });
    }

    public void saveChatResponse(String normalizedSpecies, String speciesName, String question, String normalizedQuestion, String answer) {
        String species = (normalizedSpecies != null) ? normalizedSpecies : "general";
        String cacheKey = species + "__" + normalizedQuestion.replaceAll(" ", "_");
        Map<String, Object> data = new HashMap<>();
        data.put("plantSpecies", speciesName != null ? speciesName : "General");
        data.put("normalizedSpecies", species);
        data.put("question", question);
        data.put("normalizedQuestion", normalizedQuestion);
        data.put("answer", answer);
        data.put("aiGenerated", true);
        data.put("source", "Gemini");
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_AI_CACHE).document(DOC_CHATBOT).collection("items").document(cacheKey).set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    db.collection(COLLECTION_AI_CACHE).document(DOC_CHATBOT).collection("items").document(cacheKey).get(Source.CACHE)
                            .addOnSuccessListener(doc -> chatbotMemoryCache.put(cacheKey, doc));
                });
    }

    public String computeImageHash(Bitmap bitmap) {
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 20, outputStream);
            byte[] bytes = outputStream.toByteArray();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "hash_" + System.currentTimeMillis();
        }
    }
}
