package com.eyup.library.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookPageResponse(
        List<BookResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<String> sort
) {

    public static BookPageResponse from(Page<BookResponse> page) {
        List<String> sortOrders = page.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .toList();
        return new BookPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                sortOrders
        );
    }
}
