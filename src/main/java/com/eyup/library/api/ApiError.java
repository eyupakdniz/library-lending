package com.eyup.library.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        int status,
        String path,
        Map<String, String> validationErrors,
        Instant timestamp
) {

    public static ApiError of(String code, String message, int status, String path) {
        return new ApiError(code, message, status, path, Map.of(), Instant.now());
    }

    public static ApiError validation(
            String message,
            int status,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ApiError("VALIDATION_ERROR", message, status, path, validationErrors, Instant.now());
    }

}
