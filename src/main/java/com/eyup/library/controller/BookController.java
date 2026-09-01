package com.eyup.library.controller;

import com.eyup.library.dto.BookPageResponse;
import com.eyup.library.dto.BookResponse;
import com.eyup.library.dto.CreateBookRequest;
import com.eyup.library.mapper.BookMapper;
import com.eyup.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper;

    public BookController(BookService bookService, BookMapper bookMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
    }

    @Operation(summary = "Create book. ROLE_LIBRARIAN only.")
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody CreateBookRequest request) {
        return bookMapper.toResponse(bookService.create(request));
    }

    @Operation(summary = "Get book by id. Any authenticated user is allowed.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public BookResponse getById(@PathVariable UUID id) {
        return bookMapper.toResponse(bookService.getById(id));
    }

    @Operation(summary = "List books. Any authenticated user is allowed.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BookPageResponse getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return BookPageResponse.from(bookService.getAll(pageable).map(bookMapper::toResponse));
    }

}
