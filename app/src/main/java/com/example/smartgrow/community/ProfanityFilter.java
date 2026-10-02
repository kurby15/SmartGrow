package com.example.smartgrow.community;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityFilter {
    private static final List<String> BANNED_WORDS = Arrays.asList(
        // --- TAGALOG & REGIONAL PROFANITIES ---
        "tangina", "tang-ina", "tanginamo", "taena", "wakanangsheet", "shit", "puta", "putangina", "putang-ina", "gago", "gaga", "bobo",
        "tarantado", "hayop", "pakshet", "bwisit", "pota", "kantot", "iyutan", "puke", "titi", "bayag", "utin", "bwi is sit", "let is che",
        "tanga", "tanga yarn", "kopal", "kopal ka ba boss", "b0b0", "olol",
        "bilat", "burat", "pepe", "dede", "amputa", "lintik", "demonyo", "buwisit", "kupal", "ulol", 
        "bastos", "hudas", "leche", "shunga", "pakyu", "pakyaw", "inutil", "ungas", "g@go", "b0b0", "bubu", "bubu yarn",
        "bwakanangina", "inamo", "engot", "hindot", "yawa", "giatay", "salsal", "jakol", "libog", "pekpek", "kiki",
        "piste", "atay", "botoy", "kayata", "mamatay", "putris", "pucha", "aswang", "hayup", "namo", "gagu",
        "punyeta", "gunggong", "pokpok", "pampam", "buwiset", "hampaslupa", "batugan", "mangmang",

        // --- ENGLISH PROFANITIES ---
        "fuck", "shit", "bitch", "asshole", "dick", "pussy", "bastard", "crap", "damn", "fucker",
        "hell", "slut", "whore", "idiot", "stupid", "moron", "dumbass", "faggot", "nigger", "cunt",
        "motherfucker", "bastardo", "ass", "b!tch", "f*ck", "sh!t", "nigga", "fck", "stfu", "lmfao",
        "retard", "jackass", "piss", "vagina", "penis",

        // --- KOREAN PROFANITIES (Hangul & Romanized) ---
        "ssibal", "gaesaekki", "byeongsin", "jiral", "jot", "saekki", "ssib-al", "gaesae", "ssib",
        "씨발", "개새끼", "병신", "지랄", "좆", "새끼", "미친놈", "미친년", "엿먹어", "닥쳐",
        "mi-chin-nom", "michin", "sae-kki", "jog-gat-ne",

        // --- JAPANESE PROFANITIES (Kanji/Kana & Romanized) ---
        "baka", "aho", "kuso", "shine", "yarou", "temee", "kisama", "kuzure", "yariman", "yarichin",
        "馬鹿", "阿呆", "糞", "死ね", "野郎", "てめえ", "きさま", "くそ", "しね", "ばか", "あほ",
        "manko", "chinko", "kuso", "bakayarou"
    );

    /**
     * Checks if the text contains any profanity.
     * Uses aggressive normalization to catch obfuscated words.
     */
    public static boolean hasProfanity(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String cleanText = text.toLowerCase();
        // Normalizes text by removing symbols people use to hide bad words (e.g. "f.u.c.k" or "p u t a")
        String completelyNormalized = cleanText.replaceAll("[\\s\\.\\-_\\*\\@\\!\\#\\$\\^]", "");

        for (String banned : BANNED_WORDS) {
            // Check normalized sequence (handles bypass tricks)
            if (completelyNormalized.contains(banned)) {
                return true;
            }
            
            // Check word boundaries for original text
            String regex = "\\b" + Pattern.quote(banned) + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(cleanText).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filters the text by replacing detected profanity with asterisks.
     * Used mainly for existing comments/posts display.
     */
    public static String filterText(String text) {
        if (text == null || text.isEmpty()) return text;
        
        String filteredText = text;
        for (String banned : BANNED_WORDS) {
            // Case-insensitive replacement with word boundary check
            String regex = "(?i)\\b" + Pattern.quote(banned) + "\\b";
            filteredText = filteredText.replaceAll(regex, "****");
        }
        
        return filteredText;
    }
}
