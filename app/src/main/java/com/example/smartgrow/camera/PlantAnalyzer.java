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

        if (contextHistory != null && !contextHistory.trim().isEmpty()) {
            systemMsg.put("content", "You are SmartGrow Assistant, powered by DeepSeek.\n\n" +
                    "Below is the conversation history and plant details from previous chats:\n" +
                    "--- PREVIOUS CONVERSATION & PLANT CONTEXT ---\n" +
                    contextHistory + "\n" +
                    "---------------------------------------------\n\n" +
                    "Your job now is to answer follow-up questions while maintaining memory of what was discussed previously.\n" +
                    "Answer ONLY questions related to plants, plant health, diseases, pests, watering, fertilizer, soil, sunlight, pruning, propagation, repotting, or general care.\n\n" +
                    "If the user asks a completely unrelated question, reply only with:\n" +
                    "\"Sorry, I can only answer questions related to plants or the plant you previously uploaded.\"");
        } else {
            systemMsg.put("content", "You are SmartGrow Assistant, an AI assistant for the SmartGrow application powered by DeepSeek.\n\n" +
                    "Your ONLY purpose is to help users with plants.\n\n" +
                    "If the user's question is NOT related to plants, gardening, farming, or plant care, reply only with:\n" +
                    "\"Sorry, I can only answer questions related to plants and plant care. Please ask me something about plants.\"");
        }
        messages.put(systemMsg);

        // Include the latest user question
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
                        .append("5. Do NOT output raw JSON or structured Plant Profiles unless specifically asked.");
            } else {
                basePromptBuilder.append("You are an expert botanical computer vision engine and global ecology system.\n\n")
                        .append("TASK INSTRUCTIONS:\n")
                        .append("1. First, check if the image contains a plant (real or artificial/fake/plastic).\n")
                        .append("   - If NO plant or botanical element is present, return JSON: {\"is_plant\": false}\n\n")
                        .append("2. ARTIFICIAL PLANT INSPECTION:\n")
                        .append("   - Inspect if the plant is ARTIFICIAL / FAUX / PLASTIC / SYNTHETIC / SILK.\n")
                        .append("   - Look for plastic gloss, injection molding seams, unnatural leaf patterns, synthetic stems, or fabric textures.\n")
                        .append("   - Set \"is_artificial\" to true if artificial, otherwise false.\n")
                        .append("   - In \"artificial_details\", list specific reasons why it is identified as artificial. If real, leave this array empty.\n\n")
                        .append("3. GLOBAL BOTANICAL DISTRIBUTION & HABITAT MANDATE:\n")
                        .append("   - Identify species taxonomy (scientific name) to determine its exact WORLDWIDE distribution.\n")
                        .append("   - You MUST supply 4 to 20 representative coordinate pins in \"distribution_coordinates\" covering ALL countries and continents where this species naturally occurs OR has been introduced/naturalized.\n")
                        .append("   - In \"habitat\", synthesize the natural environment and ecological niches corresponding to ALL countries where the plant is pinned.\n")
                        .append("   - Provide full distribution_text summarizing all global range countries where pins are set.\n")
                        .append("   - Distinguish distribution types strictly as: 'Native', 'Introduced', 'Naturalized', 'Invasive', or 'Cultivated'.\n\n")
                        .append("4. COMMON PROBLEMS SCHEMA MANDATE:\n")
                        .append("   - Provide 2 to 4 common health problems or diseases for this specific plant species.\n")
                        .append("   - Each item in \"common_problems\" MUST contain detailed fields: title, description, symptom_analysis, disease_cause, solutions, and prevention.\n\n")
                        .append("5. CARE & HOW-TOS MANDATE:\n")
                        .append("   - Detail the essential care conditions (sunlight, soil, temperature, humidity, hardiness_zones).\n")
                        .append("   - Detail specific how-to guides for pruning, propagation, and repotting in the \"how_tos\" object.\n\n")
                        .append("6. Return ONLY pure JSON matching EXACTLY this structure:\n")
                        .append("{\n")
                        .append("  \"is_plant\": true,\n")
                        .append("  \"is_artificial\": false,\n")
                        .append("  \"artificial_details\": [],\n")
                        .append("  \"plant_profile\": {\n")
                        .append("    \"name\": \"Common Name (Scientific Name)\",\n")
                        .append("    \"scientific_name\": \"Scientific Name\",\n")
                        .append("    \"philippine_name\": \"Local Philippine Name or N/A\",\n")
                        .append("    \"aliases\": \"Provide 3 to 5 real alternative common names specifically for THIS exact species separated by commas. Do not invent unrelated plant names.\",\n")
                        .append("    \"origin\": \"Native origin region/countries\",\n")
                        .append("    \"distribution_text\": \"Complete worldwide geographic distribution listing all country locations pinned on map.\",\n")
                        .append("    \"habitat\": \"Detailed habitat summary encompassing environmental conditions across all countries where it is pinned.\",\n")
                        .append("    \"distribution_confidence\": \"High\",\n")
                        .append("    \"distribution_coordinates\": [\n")
                        .append("      {\n")
                        .append("        \"latitude\": -14.2350,\n")
                        .append("        \"longitude\": -51.9253,\n")
                        .append("        \"title\": \"Brazil\",\n")
                        .append("        \"region\": \"South America\",\n")
                        .append("        \"country\": \"Brazil\",\n")
                        .append("        \"distribution_type\": \"Native\",\n")
                        .append("        \"is_native\": true,\n")
                        .append("        \"confidence\": \"High\",\n")
                        .append("        \"snippet\": \"Native tropical rainforest habitat.\",\n")
                        .append("        \"description\": \"Part of native range in South America.\"\n")
                        .append("      }\n")
                        .append("    ],\n")
                        .append("    \"type\": \"Plant Type (e.g. Indoor Herb, Shrub, Succulent)\",\n")
                        .append("    \"pet_toxicity\": \"Non-toxic to pets / Toxic to pets\",\n")
                        .append("    \"weed_potential\": \"Low weed potential / Invasive weed\",\n")
                        .append("    \"lifespan\": \"Perennial / Annual\",\n")
                        .append("    \"care_difficulty\": \"Easy / Moderate / Hard\"\n")
                        .append("  },\n")
                        .append("  \"common_problems\": [\n")
                        .append("    {\n")
                        .append("      \"title\": \"Aged yellow and dry\",\n")
                        .append("      \"description\": \"Natural aging can cause leaves to turn yellow and dry out.\",\n")
                        .append("      \"symptom_analysis\": \"When plants have progressed through their natural developmental stages, leaves will start to yellow, droop, and turn papery brown.\",\n")
                        .append("      \"disease_cause\": \"At the end of its life, genetic coding within the plant increases the production of ethylene, leading to natural cell breakdown.\",\n")
                        .append("      \"solutions\": \"If yellowing is a natural progression due to age, nothing can be done to stop it. Prune dead leaves to keep the plant clean.\",\n")
                        .append("      \"prevention\": \"To prolong leaf life, ensure proper water, adequate sunlight, and balanced fertilization.\",\n")
                        .append("      \"image_url\": \"\"\n")
                        .append("    }\n")
                        .append("  ],\n")
                        .append("  \"health_scanner\": {\n")
                        .append("    \"status\": \"Healthy / Artificial / Diseased / Indeterminate\",\n")
                        .append("    \"confidence\": \"85%\",\n")
                        .append("    \"health_score\": 85,\n")
                        .append("    \"tissue_damage\": \"Description of damage or N/A\"\n")
                        .append("  },\n")
                        .append("  \"hydration_scanner\": {\n")
                        .append("    \"turgor_pressure\": \"High/Optimal/Wilting/N/A\",\n")
                        .append("    \"moisture_estimate\": \"Dry/Moist/Saturated/N/A\"\n")
                        .append("  },\n")
                        .append("  \"ecosystem\": {\n")
                        .append("    \"humidity_preference\": \"High (60-80%) / Medium (40-60%) / Low\",\n")
                        .append("    \"temp_range\": \"18-30°C\",\n")
                        .append("    \"soil\": \"Well-draining, nutrient-rich potting mix with perlite\",\n")
                        .append("    \"soil_type\": \"Well-draining potting mix\",\n")
                        .append("    \"hardiness_zones\": \"9-11\",\n")
                        .append("    \"sunlight\": \"Bright indirect light\"\n")
                        .append("  },\n")
                        .append("  \"how_tos\": {\n")
                        .append("    \"pruning\": \"Trim yellowed or damaged leaves near the base using sterilized shears during spring/summer.\",\n")
                        .append("    \"propagation\": \"Take 4-6 inch stem cuttings below a node, root in water or moist soil for 2-3 weeks.\",\n")
                        .append("    \"repotting\": \"Repot every 1-2 years in spring into a container 2 inches larger with drainage holes.\"\n")
                        .append("  },\n")
                        .append("  \"characteristics\": {\n")
                        .append("    \"ultimate_height\": \"e.g., 30 cm to 1 m\",\n")
                        .append("    \"ultimate_spread\": \"e.g., 20 cm to 50 cm\",\n")
                        .append("    \"leaf_color_hex\": \"#2E7D32\",\n")
                        .append("    \"leaf_type\": \"Evergreen\",\n")
                        .append("    \"planting_time\": \"Spring\"\n")
                        .append("  },\n")
                        .append("  \"care_profile\": {\n")
                        .append("    \"difficulty\": \"Easy\",\n")
                        .append("    \"watering_frequency\": \"Water when top inch is dry\",\n")
                        .append("    \"propagation_method\": \"Stem cuttings\"\n")
                        .append("  },\n")
                        .append("  \"extra_details\": {\n")
                        .append("    \"uses\": \"Decorative / Ornamental\",\n")
                        .append("    \"uses_disclaimer\": \"\",\n")
                        .append("    \"adaptation_strategies\": \"Drought tolerant\",\n")
                        .append("    \"ecological_application\": \"Air purifier\",\n")
                        .append("    \"history_and_legends\": \"Historical context\",\n")
                        .append("    \"name_story\": \"Etymology\",\n")
                        .append("    \"symbolism\": \"Symbolic meaning\"\n")
                        .append("  },\n")
                        .append("  \"symptoms_checklist\": [],\n")
                        .append("  \"intervention_strategy\": {\n")
                        .append("    \"immediate_action\": \"No watering required if artificial.\",\n")
                        .append("    \"long_term_care\": \"Dust periodically.\"\n")
                        .append("  },\n")
                        .append("  \"smart_grow_lesson\": \"Short educational note\"\n")
                        .append("}\n\n")
                        .append("OUTPUT REQUIREMENTS: Output ONLY pure valid JSON. Do not include introductory text or markdown commentary outside the JSON object.");

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
            String systemInstructions = "You are SmartGrow Assistant, backed up by Gemini. Your ONLY purpose is to help users with plants. " +
                    "If the user's question is NOT related to plants, gardening, farming, or care, reply with: " +
                    "\"Sorry, I can only answer questions related to plants and plant care.\" ";

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
            JSONArray artificialDetails = root.optJSONArray("artificial_details");
            String localPhName = profile.optString("philippine_name", "N/A");
            String origin = profile.optString("origin", "N/A");
            String distributionText = profile.optString("distribution_text", "N/A");
            String habitatText = profile.optString("habitat", "N/A");

            JSONObject health = root.optJSONObject("health_scanner");
            JSONObject hydration = root.optJSONObject("hydration_scanner");
            JSONObject ecosystem = root.optJSONObject("ecosystem");
            JSONObject howTos = root.optJSONObject("how_tos");
            JSONArray symptoms = root.optJSONArray("symptoms_checklist");
            JSONObject intervention = root.optJSONObject("intervention_strategy");

            String commonName = nameString;
            String scientificName = "Not specified";
            if (nameString.contains("(") && nameString.contains(")")) {
                int openParen = nameString.indexOf("(");
                int closeParen = nameString.indexOf(")");
                if (openParen < closeParen) {
                    commonName = nameString.substring(0, openParen).trim();
                    scientificName = nameString.substring(openParen + 1, closeParen).trim();
                }
            }

            StringBuilder sb = new StringBuilder();

            if (isArtificial) {
                sb.append("⚠️ Artificial / Fake Plant Detected\n");
                sb.append("This image appears to be an artificial or faux ").append(commonName).append(".\n");

                if (artificialDetails != null && artificialDetails.length() > 0) {
                    sb.append("Noticeable details:\n");
                    for (int i = 0; i < artificialDetails.length(); i++) {
                        String detail = artificialDetails.optString(i, "");
                        if (!detail.trim().isEmpty()) {
                            sb.append("• ").append(detail).append("\n");
                        }
                    }
                }
                sb.append("\nHere is general care info for the real species:\n\n");
            }

            sb.append("🌿 Plant Profile\n");
            sb.append("• Name: ").append(commonName).append("\n");
            sb.append("• Scientific Name: ").append(scientificName).append("\n");

            if (!"N/A".equalsIgnoreCase(localPhName) && !localPhName.trim().isEmpty()) {
                sb.append("• Local Name: ").append(localPhName).append("\n");
            }

            if (profile.has("type")) {
                sb.append("• Type: ").append(profile.optString("type", "N/A")).append("\n");
            }
            if (ecosystem != null && ecosystem.has("soil_type")) {
                sb.append("• Adaptability: ").append(ecosystem.optString("soil_type", "N/A")).append(" Adapted\n");
            }
            if (!"N/A".equalsIgnoreCase(origin) && !origin.trim().isEmpty()) {
                sb.append("• Native Origin: ").append(origin).append("\n");
            }
            if (!"N/A".equalsIgnoreCase(distributionText) && !distributionText.trim().isEmpty()) {
                sb.append("• Geographic Distribution: ").append(distributionText).append("\n");
            }
            if (!"N/A".equalsIgnoreCase(habitatText) && !habitatText.trim().isEmpty()) {
                sb.append("• Global Habitat: ").append(habitatText).append("\n");
            }
            sb.append("\n");

            if (health != null) {
                sb.append("🩺 Health Assessment\n");
                sb.append("• Condition: ").append(isArtificial ? "Artificial / Plastic Plant" : health.optString("status", "N/A")).append("\n");
                sb.append("• Confidence: ").append(health.optString("confidence", "N/A")).append("\n\n");
            }

            if (hydration != null || ecosystem != null) {
                sb.append("💧 Care Guide\n");
                if (hydration != null && !isArtificial) {
                    sb.append("• Watering: ").append(hydration.optString("turgor_pressure", "N/A"))
                            .append(" indications / ").append(hydration.optString("moisture_estimate", "N/A")).append(" soil target\n");
                }
                if (ecosystem != null) {
                    sb.append("• Soil: ").append(ecosystem.optString("soil_type", "N/A")).append("\n");
                    sb.append("• Temperature: ").append(ecosystem.optString("temp_range", "N/A")).append(" °C\n");
                    sb.append("• Humidity: ").append(ecosystem.optString("humidity_preference", "N/A")).append("\n");
                }
            }

            if (!isArtificial && symptoms != null && symptoms.length() > 0) {
                boolean hasRealSymptoms = false;
                StringBuilder symptomsBuilder = new StringBuilder();
                for (int i = 0; i < symptoms.length(); i++) {
                    String symptom = symptoms.optString(i, "");
                    if (!symptom.trim().isEmpty() && !"N/A".equalsIgnoreCase(symptom)) {
                        symptomsBuilder.append("• ").append(symptom).append("\n");
                        hasRealSymptoms = true;
                    }
                }
                if (hasRealSymptoms) {
                    sb.append("\n⚠️ Problems Detected\n").append(symptomsBuilder);
                }
            }

            if (intervention != null) {
                sb.append("\n✅ Recommendations\n");
                if (isArtificial) {
                    sb.append("• Immediate: Keep free of dust with a damp cloth.\n");
                    sb.append("• Long-term: Keep away from direct high heat to prevent plastic degradation.\n\n");
                } else {
                    sb.append("• Immediate: ").append(intervention.optString("immediate_action", "N/A")).append("\n");
                    sb.append("• Long-term: ").append(intervention.optString("long_term_care", "N/A")).append("\n\n");
                }
            }

            String lesson = root.optString("smart_grow_lesson", "");
            if (!lesson.trim().isEmpty()) {
                sb.append("💡 SmartGrow Lesson: ").append(lesson);
            }

            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "JSON parsing error", e);
            if (jsonRawString.contains("does not appear to contain a plant") || jsonRawString.contains("\"is_plant\": false")) {
                return REJECT_MESSAGE;
            }
            return jsonRawString.replaceAll("[\\{\\}\"\\[\\]]", "")
                    .replaceAll("(?m)^[ \t]*[a-zA-Z_]+:\\s*", "• ")
                    .trim();
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