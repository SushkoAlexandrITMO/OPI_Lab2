package ru.itmo.lab1.exception;

import java.util.Map;

public class BadRequestException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public BadRequestException(String message) {
        this(message, Map.of());
    }

    public BadRequestException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public static BadRequestException field(String field, String message) {
        return new BadRequestException(message, Map.of(field, message));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
