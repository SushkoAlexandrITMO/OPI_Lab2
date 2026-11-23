package ru.itmo.lab1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        Map<String, Object> details
) {
    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, Map.of(), Map.of());
    }
}
