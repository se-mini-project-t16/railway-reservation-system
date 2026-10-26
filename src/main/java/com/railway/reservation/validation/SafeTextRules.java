package com.railway.reservation.validation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side input rules for free-text fields (RRS-SR-003).
 * Rejects HTML/script markup (XSS) and common SQL-injection payloads.
 * Normal names such as "O'Brien" or "Anne-Marie" are allowed.
 * Database access still uses parameterized queries; this is defence in depth.
 */
public final class SafeTextRules {

    private static final List<Pattern> BLOCKED = List.of(
            // HTML / script markup and encoded variants
            p("[<>]"),
            p("%3c|%3e|&lt|&gt|&#x?0*(60|3c|62|3e)"),
            p("javascript\\s*:"),
            p("vbscript\\s*:"),
            p("data\\s*:\\s*text/html"),
            p("\\bon[a-z]{3,}\\s*="),
            // control characters
            p("\\p{Cntrl}"),
            // SQL comments
            p("--"),
            p("/\\*"),
            p("\\*/"),
            // stacked statements
            p(";\\s*(select|insert|update|delete|drop|alter|create|truncate|exec|execute|union)\\b"),
            // quote followed by SQL keyword, e.g. admin' OR '1'='1
            p("['\"`]\\s*(or|and|union|select|insert|update|delete|drop|having|waitfor|sleep|exec)\\b"),
            // tautologies such as 1 OR 1=1
            p("\\b(or|and)\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+"),
            p("\\bunion\\s+(all\\s+)?select\\b"),
            p("\\bdrop\\s+(table|database)\\b"),
            p("\\b(sleep|benchmark)\\s*\\("),
            p("\\bwaitfor\\s+delay\\b"),
            p("\\bxp_cmdshell\\b"),
            p("\\binformation_schema\\b")
    );

    private SafeTextRules() {}

    private static Pattern p(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /** Null is treated as safe; use @NotBlank to require a value. */
    public static boolean isSafe(String value) {
        if (value == null) {
            return true;
        }
        for (Pattern pattern : BLOCKED) {
            if (pattern.matcher(value).find()) {
                return false;
            }
        }
        return true;
    }

    /** Throws IllegalArgumentException (mapped to 400) without echoing the payload back. */
    public static void requireSafe(String fieldLabel, String value) {
        if (!isSafe(value)) {
            throw new IllegalArgumentException(fieldLabel + " contains characters or patterns that are not allowed.");
        }
    }
}
