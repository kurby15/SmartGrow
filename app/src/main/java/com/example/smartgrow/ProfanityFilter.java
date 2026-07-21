package com.example.smartgrow;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityFilter {
    private static final List<String> BANNED_WORDS = Arrays.asList(
        "tangina", "puta", "gago", "bobo", "tarantado", "hayop", "pakshet", "bwisit", 
        "pota", "kantot", "iyutan", "puke", "titi", "bayag", "bilat", "burat", "pepe", 
        "dede", "amputa", "lintik", "demonyo", "buwisit", "kupal", "ulol", "bastos",
        "hudas", "leche", "shunga", "pakyu",

        "fuck", "shit", "bitch", "asshole", "dick", "pussy", "bastard", "crap", "damn",
        "hell", "slut", "whore", "idiot", "stupid", "moron", "dumbass", "faggot", "nigger"
    );

    public static boolean hasProfanity(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String cleanText = text.toLowerCase();

        for (String banned : BANNED_WORDS) {
            // \b is a word boundary. This prevents "hello" from being flagged because of "hell"
            String regex = "\\b" + Pattern.quote(banned) + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(cleanText).find()) {
                return true;
            }
        }

        return false;
    }
}
