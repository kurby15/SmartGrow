package com.example.smartgrow.core;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeminiCacheService {
    private static final String TAG = "GeminiCacheService";
    private static final String COLLECTION_AI_CACHE = "ai_cache";
    private static final String COLLECTION_PLANTS = "plants";
    private static final String COLLECTION_CHATBOT = "chatbot";
    private static final String COLLECTION_IDENTIFICATION = "identification";

    private static final double PLANT_IDENTIFICATION_THRESHOLD = 0.80;
    private static final double SIMILARITY_THRESHOLD = 0.70;

    private final Map<String, String> plantMemoryCache = new HashMap<>();
    private final Map<String, String> chatbotMemoryCache = new HashMap<>();
    private final Map<String, List<ChatCacheEntry>> speciesChatHistoryMemory = new HashMap<>();

    // Memory cache for identification (image hash -> species info)
    private final Map<String, String[]> identificationMemoryCache = new HashMap<>();

    private final FirebaseFirestore db;
    private static GeminiCacheService instance;

    private static class ChatCacheEntry {
        String normalizedQuestion;
        String answer;
        ChatCacheEntry(String q, String a) {
            this.normalizedQuestion = q;
            this.answer = a;
        }
    }

    private GeminiCacheService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized GeminiCacheService getInstance() {
        if (instance == null) {
            instance = new GeminiCacheService();
        }
        return instance;
    }

    public double getIdentificationThreshold() {
        return PLANT_IDENTIFICATION_THRESHOLD;
    }

    public String normalizeSpecies(String species) {
        if (species == null) return "unknown_species";
        return species.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }

    public String normalizeQuestion(String question) {
        if (question == null) return "";
        return question.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Computes a fast hash of a bitmap to detect identical or near-identical images.
     */
    public String computeImageHash(Bitmap bitmap) {
        try {
            // Scale way down to 64x64 to create a "perceptual" hash faster
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 40, outputStream);
            byte[] bytes = outputStream.toByteArray();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "hash_" + System.currentTimeMillis();
        }
    }

    public String[] getIdentificationFromMemory(String hash) {
        return identificationMemoryCache.get(hash);
    }

    public Task<DocumentSnapshot> getCachedIdentification(String hash) {
        Log.d(TAG, "Checking identification cache: " + hash);
        return db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_IDENTIFICATION)
                .collection("items")
                .document(hash)
                .get(Source.DEFAULT)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String name = task.getResult().getString("speciesName");
                        String normalized = task.getResult().getString("normalizedSpecies");
                        if (name != null && normalized != null) {
                            identificationMemoryCache.put(hash, new String[]{name, normalized});
                        }
                    }
                    return task.getResult();
                });
    }

    public void saveIdentificationCache(String hash, String speciesName, String normalizedSpecies) {
        Map<String, Object> data = new HashMap<>();
        data.put("speciesName", speciesName);
        data.put("normalizedSpecies", normalizedSpecies);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_IDENTIFICATION)
                .collection("items")
                .document(hash)
                .set(data, SetOptions.merge());

        identificationMemoryCache.put(hash, new String[]{speciesName, normalizedSpecies});
    }

    public String getPlantFromMemory(String normalizedSpecies) {
        return plantMemoryCache.get(normalizedSpecies);
    }

    public String getChatbotFromMemory(String normalizedSpecies, String normalizedQuestion) {
        String cacheKey = normalizedSpecies + "__" + normalizedQuestion.replaceAll(" ", "_");
        if (chatbotMemoryCache.containsKey(cacheKey)) {
            return chatbotMemoryCache.get(cacheKey);
        }

        List<ChatCacheEntry> entries = speciesChatHistoryMemory.get(normalizedSpecies);
        if (entries != null) {
            for (ChatCacheEntry entry : entries) {
                if (calculateSimilarity(normalizedQuestion, entry.normalizedQuestion) >= SIMILARITY_THRESHOLD) {
                    return entry.answer;
                }
            }
        }
        return null;
    }

    public void preFetchChatbotAnswers(String normalizedSpecies) {
        db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_CHATBOT)
                .collection("items")
                .whereEqualTo("normalizedSpecies", normalizedSpecies)
                .get(Source.DEFAULT)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatCacheEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String q = doc.getString("normalizedQuestion");
                        String a = doc.getString("answer");
                        if (q != null && a != null) {
                            entries.add(new ChatCacheEntry(q, a));
                            chatbotMemoryCache.put(normalizedSpecies + "__" + q.replaceAll(" ", "_"), a);
                        }
                    }
                    speciesChatHistoryMemory.put(normalizedSpecies, entries);
                });
    }

    public Task<DocumentSnapshot> getCachedPlant(String normalizedSpecies) {
        return db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_PLANTS)
                .collection("items")
                .document(normalizedSpecies)
                .get(Source.DEFAULT)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String json = task.getResult().getString("rawAnalysisJson");
                        if (json != null) plantMemoryCache.put(normalizedSpecies, json);
                    }
                    return task.getResult();
                });
    }

    public void savePlantCache(String normalizedSpecies, Map<String, Object> data) {
        data.put("normalizedSpecies", normalizedSpecies);
        data.put("aiGenerated", true);
        data.put("source", "Gemini");
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_PLANTS)
                .collection("items")
                .document(normalizedSpecies)
                .set(data, SetOptions.merge());

        if (data.containsKey("rawAnalysisJson")) {
            plantMemoryCache.put(normalizedSpecies, (String) data.get("rawAnalysisJson"));
        }
    }

    public Task<DocumentSnapshot> getCachedChatResponse(String normalizedSpecies, String normalizedQuestion) {
        String cacheKey = normalizedSpecies + "__" + normalizedQuestion.replaceAll(" ", "_");
        return db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_CHATBOT)
                .collection("items")
                .document(cacheKey)
                .get(Source.DEFAULT)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String answer = task.getResult().getString("answer");
                        if (answer != null) {
                            chatbotMemoryCache.put(cacheKey, answer);
                            updateSpeciesMemory(normalizedSpecies, normalizedQuestion, answer);
                        }
                    }
                    return task.getResult();
                });
    }

    public void saveChatResponse(String normalizedSpecies, String speciesName, String question, String normalizedQuestion, String answer) {
        String cacheKey = normalizedSpecies + "__" + normalizedQuestion.replaceAll(" ", "_");
        Map<String, Object> data = new HashMap<>();
        data.put("plantSpecies", speciesName);
        data.put("normalizedSpecies", normalizedSpecies);
        data.put("question", question);
        data.put("normalizedQuestion", normalizedQuestion);
        data.put("answer", answer);
        data.put("aiGenerated", true);
        data.put("source", "Gemini");
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_AI_CACHE)
                .document(COLLECTION_CHATBOT)
                .collection("items")
                .document(cacheKey)
                .set(data, SetOptions.merge());

        chatbotMemoryCache.put(cacheKey, answer);
        updateSpeciesMemory(normalizedSpecies, normalizedQuestion, answer);
    }

    private void updateSpeciesMemory(String species, String question, String answer) {
        List<ChatCacheEntry> entries = speciesChatHistoryMemory.get(species);
        if (entries == null) entries = new ArrayList<>();
        entries.add(new ChatCacheEntry(question, answer));
        speciesChatHistoryMemory.put(species, entries);
    }

    private double calculateSimilarity(String q1, String q2) {
        Set<String> k1 = getKeywords(q1);
        Set<String> k2 = getKeywords(q2);
        if (k1.isEmpty() && k2.isEmpty()) return 1.0;
        if (k1.isEmpty() || k2.isEmpty()) return 0;
        int intersection = 0;
        for (String k : k1) {
            if (k2.contains(k)) intersection++;
        }
        int union = k1.size() + k2.size() - intersection;
        return (double) intersection / union;
    }

    private Set<String> getKeywords(String text) {
        String[] words = text.split(" ");
        Set<String> keywords = new HashSet<>();
        List<String> stopWords = Arrays.asList("how", "do", "i", "my", "is", "are", "the", "a", "an", "to", "for", "of", "with", "should", "often", "when", "what", "can", "you", "tell", "me", "about");
        for (String w : words) {
            if (!stopWords.contains(w) && w.length() > 2) {
                keywords.add(w);
            }
        }
        return keywords;
    }
}
