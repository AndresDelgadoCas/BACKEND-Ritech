package com.ritechsas.backend.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Prevents plaintext credentials from being stored or exposed in support conversations. */
public final class SupportTextSanitizer {
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(
        "(?i)((?:contrase(?:ñ|n)a|clave|password|passcode)(?:\\s+(?:nueva|temporal|provisional))?\\s*(?:es|:|=)\\s*)([A-Za-z0-9!@#$%^&*_.-]{4,})"
    );

    private SupportTextSanitizer() {}

    public static String redactCredentials(String value) {
        if (value == null || value.isEmpty()) return value;
        Matcher matcher = CREDENTIAL_PATTERN.matcher(value);
        StringBuffer safe = new StringBuffer();
        while (matcher.find()) {
            String candidate = matcher.group(2);
            boolean looksLikeCredential = candidate.length() >= 16
                || candidate.matches(".*\\d.*")
                || candidate.matches(".*[^A-Za-z0-9].*");
            String replacement = looksLikeCredential
                ? matcher.group(1) + "[contenido sensible oculto]"
                : matcher.group();
            matcher.appendReplacement(safe, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(safe);
        return safe.toString();
    }

    public static boolean containsPlaintextCredential(String value) {
        return value != null && !redactCredentials(value).equals(value);
    }
}
