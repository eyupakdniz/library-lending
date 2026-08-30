package com.eyup.library.domain;

import java.time.Instant;
import java.util.UUID;

public record Book(
        UUID id,
        String title,
        String isbn,
        int copies,
        Instant createdAt,
        Instant updatedAt
) { }
