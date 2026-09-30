package com.example.smartgrow.community;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityFilter {
    private static final List<String> BANNED_WORDS = Arrays.asList(
        // Tagalog Profanities & Variations
        "tangina", "tang-ina", "tanginamo", "taena", "wakanangsheet", "shit", "puta", "putangina", "putang-ina", "gago", "gaga", "bobo",
        "tarantado", "hayop", "pakshet", "bwisit", "pota", "kantot", "iyutan", "puke", "titi", "bayag", "utin", "bwi is sit", "let is che",
        "tanga", "tanga yarn", "kopal", "kopal ka ba boss", "b0b0", "olol",
        "bilat", "burat", "pepe", "dede", "amputa", "lintik", "demonyo", "buwisit", "kupal", "ulol", 
        "bastos", "hudas", "leche", "shunga", "pakyu", "pakyaw", "inutil", "ungas", "g@go", "b0b0", "bubu", "bubu yarn",

        // English Profanities & Variations
        "fuck", "shit", "bitch", "asshole", "dick", "pussy", "bastard", "crap", "damn", "fucker",
        "hell", "slut", "whore", "idiot", "stupid", "moron", "dumbass", "faggot", "nigger", "cunt"
    );

    public static boolean hasProfanity(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // Remove spaces, dots, or common characters people use to bypass filter words
        String cleanText = text.toLowerCase();
        String completelyNormalized = cleanText.replaceAll("[\\s\\.\\-_\\*\\@]", "");

        // Check 1: Whole or partial match by normalized sequence
        for (String banned : BANNED_WORDS) {
            if (completelyNormalized.contains(banned)) {
                return true;
            }
        }

        // Check 2: Word-boundary match for original text
        for (String banned : BANNED_WORDS) {
            String regex = "\\b" + Pattern.quote(banned) + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(cleanText).find()) {
                return true;
            }
        }

        return false;
    }
}
