package com.eyup.library.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Loan(
        UUID id,
        UUID bookId,
        String bookTitle,
        UUID memberId,
        LocalDate dueDate,
        LoanStatus status,
        Instant createdAt,
        Instant updatedAt
) { }
