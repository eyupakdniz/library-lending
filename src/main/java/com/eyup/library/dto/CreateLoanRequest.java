package com.eyup.library.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLoanRequest(
        @NotNull UUID bookId,

        @NotNull UUID memberId,

        @NotNull
        @Future
        LocalDate dueDate
) { }
