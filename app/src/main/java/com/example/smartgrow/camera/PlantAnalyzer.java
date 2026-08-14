package com.example.smartgrow.camera;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartgrow.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlantAnalyzer {

    private static final String TAG = "PlantAnalyzer";

    // Endpoints
    private static final String SAMBANOVA_API_URL = "https://api.sambanova.ai/v1/chat/completions";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";

    // Models
    private static final String CHAT_MODEL = "DeepSeek-V3.1";

    private static final String REJECT_MESSAGE = "Sorry, the uploaded image does not appear to contain a plant. Please upload a clear picture of a plant.";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface PlantCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public PlantAnalyzer() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void askQuestion(String userPrompt, PlantCallback callback) {
        askAI(userPrompt, null, null, callback);
    }

    public void askFollowUpQuestion(String question, String previousAnalysis, PlantCallback callback) {
        if (previousAnalysis == null || previousAnalysis.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSuccess("Please upload a plant photo first so I can help answer your questions."));
            return;
        }
        askAI(question, null, previousAnalysis, callback);
    }

    public void analyzePlant(Bitmap imageBitmap, PlantCallback callback) {
        askAI(null, imageBitmap, null, callback);
    }

    private void askAI(String question, Bitmap imageBitmap, String contextHistory, PlantCallback callback) {
        try {
            boolean isVisionRequest = (imageBitmap != null);

            if (isVisionRequest) {
                // Image requests directly route to Gemini as main vision handler
                String bodyStr = buildGeminiPayload(null, imageBitmap, null);
                sendApiRequest(bodyStr, true, true, question, imageBitmap, contextHistory, callback);
            } else {
                // Text chat requests start with SambaNova (DeepSeek)
                String bodyStr = buildSambaNovaPayload(question, contextHistory);
                sendApiRequest(bodyStr, false, false, question, imageBitmap, contextHistory, callback);
            }

        } catch (Exception e) {
            callback.onError("Error constructing AI request: " + e.getMessage());
        }
    }

    // Helper method to isolate SambaNova layout schema configuration
    private String buildSambaNovaPayload(String question, String contextHistory) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", CHAT_MODEL);
        body.put("temperature", 0.1);

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");

        if (contextHistory != null) {
            systemMsg.put("content", "You are SmartGrow Assistant, powered by DeepSeek.\n\n" +
                    "The user has uploaded a plant image context:\n" + contextHistory + "\n\n" +
                    "Your job now is to answer follow-up questions ONLY about:\n" +
                    "• The identified plant\n• Its health condition\n• Diseases, pests, watering, fertilizer, soil, sunlight, or general care\n\n" +
                    "If the user asks a completely unrelated question, reply only with:\n" +
                    "\"Sorry, I can only answer questions related to plants or the plant you previously uploaded.\"");
            messages.put(systemMsg);
        } else {
            systemMsg.put("content", "You are SmartGrow Assistant, an AI assistant for the SmartGrow application powered by DeepSeek.\n\n" +
                    "Your ONLY purpose is to help users with plants.\n\n" +
                    "If the user's question is NOT related to plants, gardening, farming, or plant care, reply only with:\n" +
                    "\"Sorry, I can only answer questions related to plants and plant care. Please ask me something about plants.\"");
            messages.put(systemMsg);
        }

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.put(userMsg);

        body.put("messages", messages);
        return body.toString();
    }

    // Helper method to isolate Gemini layout schema configuration
    private String buildGeminiPayload(String question, Bitmap imageBitmap, String contextHistory) throws JSONException {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray partsArray = new JSONArray();

        if (imageBitmap != null) {
            // Vision Prompt Configuration (Updated with philippine_name constraint)
            String basePrompt = "You are an expert agronomist and botanical computer vision engine. First, determine if the image contains a plant. " +
                    "If it is NOT a plant, return a JSON object containing exactly: {\"is_plant\": false}. " +
                    "If it IS a plant, return a JSON object conforming precisely to this scheme:\n\n" +
                    "{\n" +
                    "  \"is_plant\": true,\n" +
                    "  \"plant_profile\": {\"name\": \"Scientific (Common Name)\", \"philippine_name\": \"Local Tagalog/Philippine name if applicable, or N/A\", \"origin\": \"Origin region\", \"type\": \"Succulent/Tree/Herb etc\"},\n" +
                    "  \"health_scanner\": {\"status\": \"Healthy/Diseased/Indeterminate\", \"confidence\": \"0-100%\", \"tissue_damage\": \"Visual observation of leaf/stem integrity\"},\n" +
                    "  \"hydration_scanner\": {\"turgor_pressure\": \"High/Optimal/Low/Wilting\", \"moisture_estimate\": \"Dry/Moist/Saturated\"},\n" +
                    "  \"ecosystem\": {\"humidity_preference\": \"High/Medium/Low\", \"temp_range\": \"Min-Max Ideal Celsius\", \"soil_type\": \"Sandy/Loam/Clay/Well-draining\"},\n" +
                    "  \"symptoms_checklist\": [\"Symptom observed 1\", \"Symptom observed 2\"],\n" +
                    "  \"intervention_strategy\": {\"immediate_action\": \"Next 24h action\", \"long_term_care\": \"Maintenance adjustments\"},\n" +
                    "  \"smart_grow_lesson\": \"A quick educational takeaway about this specific condition or plant biology\"\n" +
                    "}\n\n" +
                    "Output ONLY the valid JSON structure block. Do not include markdown wrappers.";

            JSONObject textPart = new JSONObject();
            textPart.put("text", basePrompt);
            partsArray.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", bitmapToBase64(imageBitmap));
            imagePart.put("inlineData", inlineData);
            partsArray.put(imagePart);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            body.put("generationConfig", generationConfig);
        } else {
            // Text Chat Backup Instruction Setup
            String systemInstructions = "You are SmartGrow Assistant, backed up by Gemini. Your ONLY purpose is to help users with plants. " +
                    "If the user's question is NOT related to plants, gardening, farming, or care, reply with: " +
                    "\"Sorry, I can only answer questions related to plants and plant care.\" ";

            if (contextHistory != null) {
                systemInstructions += "\n\nHistorical plant profile uploaded by user for context:\n" + contextHistory;
            }

            JSONObject textPart = new JSONObject();
            textPart.put("text", systemInstructions + "\n\nUser Question: " + question);
            partsArray.put(textPart);
        }

        contentObj.put("parts", partsArray);
        contents.put(contentObj);
        body.put("contents", contents);

        return body.toString();
    }

    private void sendApiRequest(String jsonBody, final boolean isVisionRequest, final boolean isGeminiActive,
                                final String origQuestion, final Bitmap origBitmap, final String origContext,
                                final PlantCallback callback) {
        String url;
        Request.Builder requestBuilder = new Request.Builder();

        if (isGeminiActive) {
            String geminiKey = BuildConfig.GEMINI_API_KEY;
            if (geminiKey == null || geminiKey.trim().isEmpty()) {
                mainHandler.post(() -> callback.onError("Gemini API Key is missing."));
                return;
            }
            url = GEMINI_API_URL + "?key=" + geminiKey;
            requestBuilder.addHeader("Content-Type", "application/json");
        } else {
            String sambaKey = BuildConfig.SAMBANOVA_API_KEY;
            if (sambaKey == null || sambaKey.trim().isEmpty()) {
                mainHandler.post(() -> callback.onError("SambaNova API Key is missing."));
                return;
            }
            url = SAMBANOVA_API_URL;
            requestBuilder.addHeader("Authorization", "Bearer " + sambaKey)
                    .addHeader("Content-Type", "application/json");
        }

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request request = requestBuilder.url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network Error encountered: ", e);
                handleNetworkFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    String responseStr = res.body() != null ? res.body().string() : "";

                    if (!res.isSuccessful()) {
                        Log.w(TAG, "Primary API error observed (" + res.code() + "). Checking failover rules...");
                        handleNetworkFailure();
                        return;
                    }

                    try {
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        String replyText = "";

                        if (isGeminiActive) {
                            JSONArray candidates = jsonResponse.optJSONArray("candidates");
                            if (candidates != null && candidates.length() > 0) {
                                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                                if (content != null) {
                                    JSONArray parts = content.optJSONArray("parts");
                                    if (parts != null && parts.length() > 0) {
                                        replyText = parts.getJSONObject(0).optString("text", "");
                                    }
                                }
                            }
                        } else {
                            JSONArray choices = jsonResponse.optJSONArray("choices");
                            if (choices != null && choices.length() > 0) {
                                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                                if (message != null) {
                                    String rawContent = message.optString("content", "");
                                    replyText = rawContent.replaceAll("<think>[\\s\\S]*?</think>", "").trim();
                                }
                            }
                        }

                        if (replyText.isEmpty()) {
                            handleNetworkFailure();
                            return;
                        }

                        final String finalReply = replyText;
                        if (isVisionRequest) {
                            String structuredProfile = parseAndFormatPlantJson(finalReply);
                            mainHandler.post(() -> callback.onSuccess(structuredProfile));
                        } else {
                            mainHandler.post(() -> callback.onSuccess(finalReply));
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "JSON parsing error inside standard thread handler", e);
                        handleNetworkFailure();
                    }
                }
            }

            private void handleNetworkFailure() {
                if (!isGeminiActive && !isVisionRequest) {
                    Log.i(TAG, "SambaNova (DeepSeek) down. Rerouting chat request payload to Gemini fallback ecosystem.");
                    try {
                        String geminiChatBody = buildGeminiPayload(origQuestion, null, origContext);
                        sendApiRequest(geminiChatBody, false, true, origQuestion, null, origContext, callback);
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Backup Engine routing failed: " + ex.getMessage()));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("SmartGrow services are currently experiencing technical difficulties. Please check back shortly."));
                }
            }
        });
    }

    private String parseAndFormatPlantJson(String jsonRawString) {
        try {
            String cleanedJson = extractJsonBlock(jsonRawString);
            JSONObject root = new JSONObject(cleanedJson);

            if (root.has("is_plant") && !root.getBoolean("is_plant")) {
                return REJECT_MESSAGE;
            }

            JSONObject profile = root.getJSONObject("plant_profile");
            String nameString = profile.optString("name", "N/A");

            if ("N/A".equalsIgnoreCase(nameString) || nameString.trim().isEmpty()) {
                return REJECT_MESSAGE;
            }

            // Extract the new Philippine local name field
            String localPhName = profile.optString("philippine_name", "N/A");

            JSONObject health = root.getJSONObject("health_scanner");
            JSONObject hydration = root.getJSONObject("hydration_scanner");
            JSONObject ecosystem = root.getJSONObject("ecosystem");
            JSONArray symptoms = root.optJSONArray("symptoms_checklist");
            JSONObject intervention = root.getJSONObject("intervention_strategy");

            String commonName = nameString;
            String scientificName = "Not specified";
            if (nameString.contains("(") && nameString.contains(")")) {
                int openParen = nameString.indexOf("(");
                int closeParen = nameString.indexOf(")");
                if (openParen < closeParen) {
                    scientificName = nameString.substring(0, openParen).trim();
                    commonName = nameString.substring(openParen + 1, closeParen).trim();
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🌿 Plant Profile\n");
            sb.append("• Name: ").append(commonName).append("\n");
            sb.append("• Scientific Name: ").append(scientificName).append("\n");

            // Render local name only if the AI returns a valid, non-empty response
            if (!"N/A".equalsIgnoreCase(localPhName) && !localPhName.trim().isEmpty()) {
                sb.append("• Local Name: ").append(localPhName).append("\n");
            }

            sb.append("• Type: ").append(profile.optString("type", "N/A")).append("\n");
            sb.append("• Adaptability: ").append(ecosystem.optString("soil_type", "N/A")).append(" Adapted\n");
            sb.append("• Native Origin: ").append(profile.optString("origin", "N/A")).append("\n\n");

            sb.append("🩺 Health Assessment\n");
            sb.append("• Condition: ").append(health.optString("status", "N/A")).append("\n");
            sb.append("• Confidence: ").append(health.optString("confidence", "N/A")).append("\n\n");

            sb.append("💧 Care Guide\n");
            sb.append("• Watering: ").append(hydration.optString("turgor_pressure", "N/A")).append(" indications / ").append(hydration.optString("moisture_estimate", "N/A")).append(" soil target\n");
            sb.append("• Soil: ").append(ecosystem.optString("soil_type", "N/A")).append("\n");
            sb.append("• Temperature: ").append(ecosystem.optString("temp_range", "N/A")).append(" °C\n");
            sb.append("• Humidity: ").append(ecosystem.optString("humidity_preference", "N/A")).append("\n");

            if (symptoms != null && symptoms.length() > 0) {
                boolean hasRealSymptoms = false;
                StringBuilder symptomsBuilder = new StringBuilder();
                for (int i = 0; i < symptoms.length(); i++) {
                    String symptom = symptoms.getString(i);
                    if (!symptom.trim().isEmpty() && !"N/A".equalsIgnoreCase(symptom)) {
                        symptomsBuilder.append("• ").append(symptom).append("\n");
                        hasRealSymptoms = true;
                    }
                }
                if (hasRealSymptoms) {
                    sb.append("\n⚠️ Problems Detected\n").append(symptomsBuilder);
                }
            }

            sb.append("\n✅ Recommendations\n");
            sb.append("• Immediate: ").append(intervention.optString("immediate_action", "N/A")).append("\n");
            sb.append("• Long-term: ").append(intervention.optString("long_term_care", "N/A")).append("\n\n");

            String lesson = root.optString("smart_grow_lesson", "");
            if (!lesson.trim().isEmpty()) {
                sb.append("💡 SmartGrow Lesson: ").append(lesson);
            }

            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "Parsing breakdown, utilizing fallback presentation strategy", e);
            if (jsonRawString.contains("does not appear to contain a plant") || jsonRawString.contains("is_plant\": false")) {
                return REJECT_MESSAGE;
            }
            return "Plant Analysis Context Loaded:\n" + jsonRawString;
        }
    }

    private String extractJsonBlock(String raw) {
        if (raw == null) return "{}";
        raw = raw.trim();

        Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return raw.substring(firstBrace, lastBrace + 1).trim();
        }

        return raw;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
