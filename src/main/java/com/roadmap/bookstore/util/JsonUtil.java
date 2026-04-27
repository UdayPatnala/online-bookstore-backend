package com.roadmap.bookstore.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Map<String, String> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }

        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON object");
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        Map<String, String> result = new HashMap<>();

        if (body.isEmpty()) {
            return result;
        }

        for (String pair : splitTopLevel(body, ',')) {
            int colonIndex = findTopLevelColon(pair);
            if (colonIndex <= 0) {
                throw new IllegalArgumentException("Invalid JSON key/value pair");
            }

            String keyPart = pair.substring(0, colonIndex).trim();
            String valuePart = pair.substring(colonIndex + 1).trim();

            String key = unquote(keyPart);
            String value = unquote(valuePart);
            result.put(key, value);
        }

        return result;
    }

    public static String requireString(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value;
    }

    public static long requireLong(Map<String, String> map, String key) {
        String value = requireString(map, key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid long value for field: " + key);
        }
    }

    public static int requireInt(Map<String, String> map, String key) {
        String value = requireString(map, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid int value for field: " + key);
        }
    }

    public static double requireDouble(Map<String, String> map, String key) {
        String value = requireString(map, key);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number value for field: " + key);
        }
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch == '"' && !isEscaped(value, i)) {
                inQuotes = !inQuotes;
            }

            if (ch == delimiter && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        parts.add(current.toString().trim());
        return parts;
    }

    private static int findTopLevelColon(String pair) {
        boolean inQuotes = false;
        for (int i = 0; i < pair.length(); i++) {
            char ch = pair.charAt(i);
            if (ch == '"' && !isEscaped(pair, i)) {
                inQuotes = !inQuotes;
            }
            if (ch == ':' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEscaped(String value, int quoteIndex) {
        int slashCount = 0;
        for (int i = quoteIndex - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private static String unquote(String raw) {
        String value = raw.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
            value = value
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\\\", "\\");
        }
        return value;
    }
}