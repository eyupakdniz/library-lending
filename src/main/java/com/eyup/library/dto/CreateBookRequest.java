package com.eyup.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(min = 10, max = 17)
        String isbn,

        @NotNull
        @Min(1)
        Integer copies
) { }
