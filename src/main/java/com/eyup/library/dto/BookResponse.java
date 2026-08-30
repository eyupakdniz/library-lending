package com.eyup.library.dto;

import java.time.Instant;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String isbn,
        int copies,
        Instant createdAt,
        Instant updatedAt
) { }
