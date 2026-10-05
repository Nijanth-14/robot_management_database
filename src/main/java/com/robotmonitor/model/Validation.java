package com.robotmonitor.model;

final class Validation {
    private Validation() {}

    static String text(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        value = value.trim();
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
