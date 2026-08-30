package com.eyup.library.dto;

import com.eyup.library.domain.LoanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        UUID memberId,
        LocalDate dueDate,
        LoanStatus status,
        Instant createdAt,
        Instant updatedAt
) { }
