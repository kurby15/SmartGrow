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
import java.util.ArrayList;
import java.util.List;
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

    private static final String SAMBANOVA_API_URL =
            "https://api.sambanova.ai/v1/chat/completions";

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";

    private static final String CHAT_MODEL = "DeepSeek-V3.1";

    public static final String REJECT_MESSAGE = "The image does not appear to contain a plant. Please scan a clear plant image.";
    public static final String ERROR_NON_PLANT = "NON_PLANT_DETECTED";

    private final OkHttpClient client;
    private final Handler mainHandler;

    private static final String APP_DETAILS_CONTEXT =
            "SmartGrow App Features Overview:\n\n" +
            "1. Scan/Camera: Identifies plant species and diagnoses diseases from photos using AI.\n\n" +
            "2. AI Chatbot: Your personal botanical assistant for care advice and app guidance.\n\n" +
            "3. Home Dashboard: Shows weather, garden summary, and quick care actions.\n\n" +
            "4. My Garden: A digital repository to save and track all your plants' health and progress.\n\n" +
            "5. Reminders: Set schedules for watering, fertilizing, or repotting with custom notifications.\n\n" +
            "6. Community Forum: Connect with other gardeners, share tips, and post your plant journey.\n\n" +
            "7. History: Access all your past plant scans and chat sessions anytime.\n\n" +
            "8. Profile & Settings: Manage account info, security, and app preferences.";

    public interface PlantCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public interface PlantAnalysisCallback {
        void onSuccess(String formattedResult, String rawJson);
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
        askAI(userPrompt, null, null, callback, null);
    }

    public void askFollowUpQuestion(String question, String previousAnalysis, PlantCallback callback) {
        if (previousAnalysis == null || previousAnalysis.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSuccess("Please upload a plant photo first so I can help answer your questions."));
            return;
        }
        askAI(question, null, previousAnalysis, callback, null);
    }

    public void analyzePlant(Bitmap imageBitmap, PlantCallback callback) {
        askAI(null, imageBitmap, null, callback, null);
    }

    public void analyzePlantDetailed(Bitmap imageBitmap, PlantAnalysisCallback detailedCallback) {
        askAI(null, imageBitmap, null, null, detailedCallback);
    }

    public void analyzePlantWithQuestion(String question, Bitmap imageBitmap, PlantAnalysisCallback detailedCallback) {
        askAI(question, imageBitmap, null, null, detailedCallback);
    }

    private void askAI(String question, Bitmap imageBitmap, String contextHistory,
                       PlantCallback callback, PlantAnalysisCallback detailedCallback) {
        try {
            boolean isVisionRequest = (imageBitmap != null);

            if (isVisionRequest) {
                String bodyStr = buildGeminiPayload(question, imageBitmap, contextHistory);
                sendApiRequest(bodyStr, true, true, question, imageBitmap, contextHistory, callback, detailedCallback);
            } else {
                String bodyStr = buildSambaNovaPayload(question, contextHistory);
                sendApiRequest(bodyStr, false, false, question, imageBitmap, contextHistory, callback, detailedCallback);
            }

        } catch (Exception e) {
            if (callback != null) mainHandler.post(() -> callback.onError("Error constructing AI request: " + e.getMessage()));
            if (detailedCallback != null) mainHandler.post(() -> detailedCallback.onError("Error constructing AI request: " + e.getMessage()));
        }
    }

    private String buildSambaNovaPayload(String question, String contextHistory) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", CHAT_MODEL);
        body.put("temperature", 0.1);

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");

        String baseSystemPrompt = "You are SmartGrow Assistant, powered by DeepSeek. You help users with plant care and navigating the SmartGrow app.\n\n" +
                APP_DETAILS_CONTEXT + "\n\n" +
                "Answer questions related to plants, gardening, and how to use the SmartGrow app features listed above.\n" +
                "IMPORTANT: Do not use asterisks (*) for formatting. Use plain text and double newlines for spacing.\n";

        if (contextHistory != null && !contextHistory.trim().isEmpty()) {
            systemMsg.put("content", baseSystemPrompt +
                    "Below is the conversation history and plant details from previous chats:\n" +
                    "--- PREVIOUS CONVERSATION & PLANT CONTEXT ---\n" +
                    contextHistory + "\n" +
                    "---------------------------------------------\n\n" +
                    "Answer ONLY questions related to plants (health, pests, care) or the SmartGrow app.\n" +
                    "If the user asks something completely unrelated, politely decline.");
        } else {
            systemMsg.put("content", baseSystemPrompt +
                    "If the user's question is NOT related to plants or the SmartGrow app, politely state you can only assist with plant-related topics.");
        }
        messages.put(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.put(userMsg);

        body.put("messages", messages);
        return body.toString();
    }

    private String buildGeminiPayload(String question, Bitmap imageBitmap, String contextHistory) throws JSONException {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray partsArray = new JSONArray();

        if (imageBitmap != null) {
            StringBuilder basePromptBuilder = new StringBuilder();

            // Check if there is a custom user question AND no context history
            boolean isConversationalQuestion = (question != null && !question.trim().isEmpty()) && (contextHistory == null || contextHistory.trim().isEmpty());

            if (isConversationalQuestion) {
                basePromptBuilder.append("You are SmartGrow Assistant, an expert AI plant care assistant.\n")
                        .append("The user has attached an image of their plant along with a specific question.\n\n")
                        .append("USER QUESTION: \"").append(question.trim()).append("\"\n\n")
                        .append("INSTRUCTIONS:\n")
                        .append("1. First, verify if the image contains a plant or botanical element.\n")
                        .append("2. If NO plant is found, state politely that no plant was detected.\n")
                        .append("3. Identify the plant and directly, naturally answer the user's question based on what you observe.\n")
                        .append("4. Keep the answer helpful, concise, friendly, and directly addressing their question.\n")
                        .append("5. Do NOT use asterisks (*) for bold or lists. Use double newlines for spacing.\n")
                        .append("6. Do NOT output raw JSON or structured Plant Profiles unless specifically asked.");
            } else {
                basePromptBuilder.append("You are an expert botanical computer vision engine.\n\n")
                        .append("TASK INSTRUCTIONS:\n")
                        .append("1. First, check if the image contains a plant (real or artificial/fake/plastic).\n")
                        .append("   - If NO plant or botanical element is present, return JSON: {\"is_plant\": false}\n\n")
                        .append("2. ARTIFICIAL PLANT INSPECTION:\n")
                        .append("   - Inspect if the plant is ARTIFICIAL / FAUX / PLASTIC / SYNTHETIC / SILK.\n")
                        .append("   - Set \"is_artificial\" to true if artificial, otherwise false.\n\n")
                        .append("3. HEALTH & PEST ASSESSMENT:\n")
                        .append("   - Assess the plant's health status and identify any problems or diseases.\n")
                        .append("   - Identify any pests that have damaged or could damage the uploaded plant.\n")
                        .append("   - Check specifically for signs of pest damage like chew marks (ngatngat), spots, or actual insects.\n")
                        .append("   - Provide details on what pests were detected (if any). If no pests are detected, set \"possible_pest_detected\" to \"no pest detected\".\n")
                        .append("   - Provide specific steps to avoid pests, including recommendations for organic or chemical sprays (e.g., Neem oil).\n\n")
                        .append("4. CARE GUIDE MANDATE AND GEOGRAPHICAL DISTRIBUTION MAP PINPOINTS:\n")
                        .append("   - Detail the essential care conditions (sunlight, watering, temperature).\n")
                        .append("   - Identify multiple realistic locations across the globe or regions where this plant can be found, and map them to their distribution type (Native, Cultivated, Introduced, Invasive) so they can be accurately pinned on the habitat map.\n\n")
                        .append("5. Return ONLY pure JSON matching EXACTLY this structure:\n")
                        .append("{\n")
                        .append("  \"is_plant\": true,\n")
                        .append("  \"is_artificial\": false,\n")
                        .append("  \"plant_profile\": {\n")
                        .append("    \"name\": \"Common Name\",\n")
                        .append("    \"scientific_name\": \"Scientific Name\",\n")
                        .append("    \"philippine_name\": \"Local Philippine Name or N/A\",\n")
                        .append("    \"aliases\": \"Alternative names or N/A\",\n")
                        .append("    \"confidence\": 95,\n")
                        .append("    \"match_percentage\": 95,\n")
                        .append("    \"distribution_text\": \"Geographical distribution information\",\n")
                        .append("    \"origin\": \"Origin country or region\",\n")
                        .append("    \"habitat\": \"Natural habitat description\",\n")
                        .append("    \"type\": \"Plant type (e.g. Shrub, Tree, Herb)\",\n")
                        .append("    \"plant_type\": \"Plant type description\",\n")
                        .append("    \"pet_toxicity\": \"Toxic/Non-toxic to pets description\",\n")
                        .append("    \"weed_potential\": \"Low/Medium/High weed potential\",\n")
                        .append("    \"lifespan\": \"Perennial/Annual/etc.\",\n")
                        .append("    \"care_difficulty\": \"Easy/Moderate/Hard\",\n")
                        .append("    \"difficulty_level\": \"Easy/Moderate/Hard\",\n")
                        .append("    \"care_difficulty_percentage\": 40,\n")
                        .append("    \"difficulty_percentage\": 40,\n")
                        .append("    \"distribution_coordinates\": [\n")
                        .append("      {\n")
                        .append("        \"latitude\": 14.5995,\n")
                        .append("        \"longitude\": 120.9842,\n")
                        .append("        \"title\": \"Region/City Name\",\n")
                        .append("        \"snippet\": \"Short status info text\",\n")
                        .append("        \"distribution_type\": \"Native\"\n")
                        .append("      },\n")
                        .append("      {\n")
                        .append("        \"latitude\": 35.6762,\n")
                        .append("        \"longitude\": 139.6503,\n")
                        .append("        \"title\": \"Region/City Name\",\n")
                        .append("        \"snippet\": \"Short status info text\",\n")
                        .append("        \"distribution_type\": \"Cultivated\"\n")
                        .append("      },\n")
                        .append("      {\n")
                        .append("        \"latitude\": -33.8688,\n")
                        .append("        \"longitude\": 151.2093,\n")
                        .append("        \"title\": \"Region/City Name\",\n")
                        .append("        \"snippet\": \"Short status info text\",\n")
                        .append("        \"distribution_type\": \"Introduced\"\n")
                        .append("      },\n")
                        .append("      {\n")
                        .append("        \"latitude\": 25.7617,\n")
                        .append("        \"longitude\": -80.1918,\n")
                        .append("        \"title\": \"Region/City Name\",\n")
                        .append("        \"snippet\": \"Short status info text\",\n")
                        .append("        \"distribution_type\": \"Invasive\"\n")
                        .append("      }\n")
                        .append("    ]\n")
                        .append("  },\n")
                        .append("  \"health_assessment\": {\n")
                        .append("    \"status\": \"Healthy / Diseased / etc.\",\n")
                        .append("    \"confidence\": \"95%\"\n")
                        .append("  },\n")
                        .append("  \"health_scanner\": {\n")
                        .append("    \"status\": \"Healthy / Diseased / etc.\",\n")
                        .append("    \"health_score\": 95,\n")
                        .append("    \"confidence\": 95\n")
                        .append("  },\n")
                        .append("  \"care_guide\": {\n")
                        .append("    \"watering\": \"Watering instructions\",\n")
                        .append("    \"sunlight\": \"Sunlight needs\",\n")
                        .append("    \"temperature\": \"Ideal temperature range\"\n")
                        .append("  },\n")
                        .append("  \"problems_detected\": [\n")
                        .append("    \"Problem 1\",\n")
                        .append("    \"Problem 2\"\n")
                        .append("  ],\n")
                        .append("  \"common_problems\": [\n")
                        .append("    {\n")
                        .append("      \"title\": \"Problem Name\",\n")
                        .append("      \"likelihood_percentage\": 20,\n")
                        .append("      \"description\": \"Description of the issue\",\n")
                        .append("      \"symptom_analysis\": \"What symptoms to look for\",\n")
                        .append("      \"disease_cause\": \"What causes this disease\",\n")
                        .append("      \"solutions\": \"How to treat it\",\n")
                        .append("      \"prevention\": \"How to prevent it\"\n")
                        .append("    }\n")
                        .append("  ],\n")
                        .append("  \"characteristics\": {\n")
                        .append("    \"ultimate_height\": \"e.g. 1-2 meters\",\n")
                        .append("    \"ultimate_spread\": \"e.g. 0.5-1 meter\",\n")
                        .append("    \"leaf_type\": \"e.g. Broadleaf\",\n")
                        .append("    \"planting_time\": \"e.g. Spring\",\n")
                        .append("    \"leaf_colors\": [\"#4CAF50\"],\n")
                        .append("    \"leaf_color_hex\": \"#4CAF50\"\n")
                        .append("  },\n")
                        .append("  \"ecosystem\": {\n")
                        .append("    \"temp_range\": \"e.g. 20-30°C\",\n")
                        .append("    \"hardiness_zones\": \"e.g. 9-11\",\n")
                        .append("    \"sunlight\": \"Full sun to partial shade\",\n")
                        .append("    \"soil\": \"Well-draining loam\"\n")
                        .append("  },\n")
                        .append("  \"how_tos\": {\n")
                        .append("    \"pruning\": \"Pruning instructions\",\n")
                        .append("    \"propagation\": \"Propagation instructions\",\n")
                        .append("    \"repotting\": \"Repotting instructions\"\n")
                        .append("  },\n")
                        .append("  \"extra_details\": {\n")
                        .append("    \"uses\": \"Common uses\",\n")
                        .append("    \"adaptation_strategies\": \"How it adapts\",\n")
                        .append("    \"ecological_application\": \"Ecological role\",\n")
                        .append("    \"history_and_legends\": \"Historical background\",\n")
                        .append("    \"name_story\": \"Origin of its name\",\n")
                        .append("    \"symbolism\": \"What it symbolizes\"\n")
                        .append("  },\n")
                        .append("  \"pest_info\": {\n")
                        .append("    \"possible_pest_detected\": \"Describe any pests detected or damage like chew marks (ngatngat) observed. If none, say 'no pest detected'.\",\n")
                        .append("    \"common_pests\": [\n")
                        .append("      {\n")
                        .append("        \"name\": \"Pest Name\",\n")
                        .append("        \"description\": \"Short description of the pest and its impact\"\n")
                        .append("      }\n")
                        .append("    ],\n")
                        .append("    \"how_to_avoid_pest\": \"Prevention steps and spray recommendations (e.g. Neem oil) to avoid/prevent pests\"\n")
                        .append("  },\n")
                        .append("  \"recommendations\": \"Immediate and long-term care actions\",\n")
                        .append("  \"smartqrow_lesson\": \"Short educational note\"\n")
                        .append("}\n\n")
                        .append("OUTPUT REQUIREMENTS: Output ONLY pure valid JSON. Do not include introductory text or markdown commentary outside the JSON block.");

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("responseMimeType", "application/json");
                body.put("generationConfig", generationConfig);
            }

            JSONObject textPart = new JSONObject();
            textPart.put("text", basePromptBuilder.toString());
            partsArray.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", bitmapToBase64(imageBitmap));
            imagePart.put("inlineData", inlineData);
            partsArray.put(imagePart);

        } else {
            String systemInstructions = "You are SmartGrow Assistant, backed up by Gemini. You help users with plant care and the SmartGrow app.\n\n" +
                    APP_DETAILS_CONTEXT + "\n\n" +
                    "Answer questions about plants or the SmartGrow app features listed above.\n" +
                    "IMPORTANT: Do NOT use asterisks (*) in your response. Use double newlines for clarity.";

            if (contextHistory != null && !contextHistory.trim().isEmpty()) {
                systemInstructions += "\n\nPrevious conversation & historical plant profile context:\n" + contextHistory;
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
                                final PlantCallback callback, final PlantAnalysisCallback detailedCallback) {
        String url;
        Request.Builder requestBuilder = new Request.Builder();

        if (isGeminiActive) {
            String geminiKey = BuildConfig.GEMINI_API_KEY;
            if (geminiKey == null || geminiKey.trim().isEmpty()) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Gemini API Key is missing.");
                    if (detailedCallback != null) detailedCallback.onError("Gemini API Key is missing.");
                });
                return;
            }
            url = GEMINI_API_URL + "?key=" + geminiKey;
            requestBuilder.addHeader("Content-Type", "application/json");
        } else {
            String sambaKey = BuildConfig.SAMBANOVA_API_KEY;
            if (sambaKey == null || sambaKey.trim().isEmpty()) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("SambaNova API Key is missing.");
                    if (detailedCallback != null) detailedCallback.onError("SambaNova API Key is missing.");
                });
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
                        Log.w(TAG, "Primary API error observed (" + res.code() + "). Body: " + responseStr);
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
                            boolean isConversationalQuestion = (origQuestion != null && !origQuestion.trim().isEmpty()) && (origContext == null || origContext.trim().isEmpty());

                            if (isConversationalQuestion) {
                                mainHandler.post(() -> {
                                    if (callback != null) callback.onSuccess(finalReply);
                                    if (detailedCallback != null) detailedCallback.onSuccess(finalReply, "");
                                });
                            } else {
                                String rawJsonBlock = extractJsonBlock(finalReply);

                                try {
                                    JSONObject parsedRoot = new JSONObject(rawJsonBlock);
                                    if (parsedRoot.has("is_plant") && !parsedRoot.getBoolean("is_plant")) {
                                        mainHandler.post(() -> {
                                            if (callback != null) callback.onError(REJECT_MESSAGE);
                                            if (detailedCallback != null) detailedCallback.onError(ERROR_NON_PLANT);
                                        });
                                        return;
                                    }
                                } catch (Exception ignored) {}

                                String structuredProfile = parseAndFormatPlantJson(finalReply);

                                mainHandler.post(() -> {
                                    if (callback != null) callback.onSuccess(structuredProfile);
                                    if (detailedCallback != null) detailedCallback.onSuccess(structuredProfile, rawJsonBlock);
                                });
                            }
                        } else {
                            mainHandler.post(() -> {
                                if (callback != null) callback.onSuccess(finalReply);
                                if (detailedCallback != null) detailedCallback.onSuccess(finalReply, "");
                            });
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
                        sendApiRequest(geminiChatBody, false, true, origQuestion, null, origContext, callback, detailedCallback);
                    } catch (Exception ex) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError("Backup Engine routing failed: " + ex.getMessage());
                            if (detailedCallback != null) detailedCallback.onError("Backup Engine routing failed: " + ex.getMessage());
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        String errorMsg = "SmartGrow services are currently experiencing technical difficulties. Please check back shortly.";
                        if (callback != null) callback.onError(errorMsg);
                        if (detailedCallback != null) detailedCallback.onError(errorMsg);
                    });
                }
            }
        });
    }

    /**
     * Extracts plant name suggestions and aliases from the raw JSON string generated by Gemini
     */
    public static List<String> extractPlantSuggestionsFromRawJson(String rawJson) {
        List<String> suggestions = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) return suggestions;

        try {
            int firstBrace = rawJson.indexOf('{');
            int lastBrace = rawJson.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace > firstBrace) {
                rawJson = rawJson.substring(firstBrace, lastBrace + 1).trim();
            }

            JSONObject root = new JSONObject(rawJson);
            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile != null) {
                String nameString = profile.optString("name", "");
                if (nameString.contains("(")) {
                    nameString = nameString.substring(0, nameString.indexOf("(")).trim();
                }
                if (!nameString.isEmpty()) {
                    suggestions.add(nameString);
                }

                String aliasesStr = profile.optString("aliases", "");
                if (!aliasesStr.isEmpty() && !"N/A".equalsIgnoreCase(aliasesStr)) {
                    String[] split = aliasesStr.split("[,;/]");
                    for (String alias : split) {
                        String cleaned = alias.trim();
                        if (!cleaned.isEmpty() && !suggestions.contains(cleaned)) {
                            suggestions.add(cleaned);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting plant suggestions from raw JSON", e);
        }

        return suggestions;
    }

    private String parseAndFormatPlantJson(String jsonRawString) {
        try {
            String cleanedJson = extractJsonBlock(jsonRawString);
            JSONObject root = new JSONObject(cleanedJson);

            if (root.has("is_plant") && !root.getBoolean("is_plant")) {
                return REJECT_MESSAGE;
            }

            JSONObject profile = root.optJSONObject("plant_profile");
            if (profile == null) {
                return REJECT_MESSAGE;
            }

            String nameString = profile.optString("name", "N/A");
            if ("N/A".equalsIgnoreCase(nameString) || nameString.trim().isEmpty()) {
                return REJECT_MESSAGE;
            }

            boolean isArtificial = root.optBoolean("is_artificial", false);
            String localPhName = profile.optString("philippine_name", "N/A");
            String scientificName = profile.optString("scientific_name", "Not specified");

            JSONObject health = root.optJSONObject("health_assessment");
            JSONObject care = root.optJSONObject("care_guide");
            JSONArray problems = root.optJSONArray("problems_detected");
            JSONObject pestInfo = root.optJSONObject("pest_info");
            String recommendations = root.optString("recommendations", "N/A");
            String lesson = root.optString("smartqrow_lesson", "");

            StringBuilder sb = new StringBuilder();

            if (isArtificial) {
                sb.append("Artificial / Fake Plant Detected\n\n");
            }

            // Plant Profile Section
            sb.append("🌿 Plant Profile\n\n");
            sb.append("  Name: ").append(nameString).append("\n\n");
            sb.append("  Sci Name: ").append(scientificName).append("\n\n");
            if (!"N/A".equalsIgnoreCase(localPhName) && !localPhName.trim().isEmpty()) {
                sb.append("  Local Name: ").append(localPhName).append("\n\n");
            }

            // Health Assessment Section
            if (health != null) {
                sb.append("🩺 Health Assessment\n\n");
                String status = isArtificial ? "Artificial / Plastic Plant" : health.optString("status", "N/A");
                sb.append("  Condition: ").append(status).append("\n\n");
                sb.append("  Confidence: ").append(health.optString("confidence", "N/A")).append("\n\n");
            }

            // Care Guide Section
            if (care != null) {
                sb.append("💧 Care Guide\n\n");
                if (!isArtificial) {
                    sb.append("  Watering: ").append(care.optString("watering", "N/A")).append("\n\n");
                }
                sb.append("  Sunlight: ").append(care.optString("sunlight", "N/A")).append("\n\n");
                sb.append("  Temperature: ").append(care.optString("temperature", "N/A")).append("\n\n");
            }

            // Problems Detected Section
            sb.append("⚠️ Problems Detected\n\n");
            boolean hasProblems = false;
            if (!isArtificial && problems != null && problems.length() > 0) {
                for (int i = 0; i < problems.length(); i++) {
                    String prob = problems.optString(i);
                    if (!prob.isEmpty()) {
                        sb.append("  ").append(prob).append("\n\n");
                        hasProblems = true;
                    }
                }
            }
            if (!hasProblems) {
                sb.append("  None detected\n\n");
            }

            // Pest Information Sections
            if (pestInfo != null) {
                // Common Pests Section
                sb.append("🐛 Common Pests\n\n");
                String detected = pestInfo.optString("possible_pest_detected", "no pest detected");
                JSONArray commonPests = pestInfo.optJSONArray("common_pests");
                
                sb.append("  Detected: ").append(detected).append("\n\n");
                if (commonPests != null && commonPests.length() > 0) {
                    for (int i = 0; i < commonPests.length(); i++) {
                        JSONObject pest = commonPests.optJSONObject(i);
                        if (pest != null) {
                            sb.append("  ").append(pest.optString("name", "N/A")).append("\n\n");
                        }
                    }
                }
                
                // How to Avoid Pest section
                sb.append("🛡️ How to Avoid Pest\n\n");
                sb.append("  ").append(pestInfo.optString("how_to_avoid_pest", "N/A")).append("\n\n");
            }

            // Recommendations Section
            sb.append("✅ Recommendations\n\n");
            sb.append("  ").append(recommendations).append("\n\n");

            // SmartGrow Lesson Section
            if (!lesson.trim().isEmpty()) {
                sb.append("💡 SmartGrow Lesson\n\n");
                sb.append("  ").append(lesson).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "JSON parsing error", e);
            if (jsonRawString.contains("does not appear to contain a plant") || jsonRawString.contains("\"is_plant\": false")) {
                return REJECT_MESSAGE;
            }
            return jsonRawString.replaceAll("[\\{\\}\"\\[\\]]", "").trim();
        }
    }

    private String extractJsonBlock(String raw) {
        if (raw == null) return "{}";
        raw = raw.trim();

        Pattern pattern = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.DOTALL);
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
