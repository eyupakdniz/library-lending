package com.eyup.library.controller;

import com.eyup.library.base.AbstractRestControllerTest;
import com.eyup.library.domain.Book;
import com.eyup.library.dto.CreateBookRequest;
import com.eyup.library.exception.DuplicateIsbnException;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookControllerTest extends AbstractRestControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @MockitoBean
    private BookService bookService;

    @Test
    void shouldCreateBookWhenRoleIsLibrarian() throws Exception {
        // Given
        CreateBookRequest request = createBookRequest();

        when(bookService.create(any(CreateBookRequest.class))).thenReturn(book());

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.title").value("The Pragmatic Programmer"))
                .andExpect(jsonPath("$.copies").value(2));

        verify(bookService).create(argThat(argument ->
                argument.title().equals("The Pragmatic Programmer")
                        && argument.isbn().equals("9780135957059")
                        && argument.copies() == 2
        ));
    }

    @Test
    void shouldRejectCreateBookWhenRoleIsMember() throws Exception {
        // Given
        CreateBookRequest request = createBookRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .with(user("member").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(bookService);
    }

    @Test
    void shouldReturnValidationErrorWhenCreateBookRequestIsInvalid() throws Exception {
        // Given
        CreateBookRequest request = new CreateBookRequest("", "123", 0);

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.isbn").exists())
                .andExpect(jsonPath("$.errors.copies").exists());

        verifyNoInteractions(bookService);
    }

    @Test
    void shouldReturnConflictWhenIsbnAlreadyExists() throws Exception {
        // Given
        CreateBookRequest request = createBookRequest();

        when(bookService.create(any(CreateBookRequest.class)))
                .thenThrow(new DuplicateIsbnException("Book already exists with same isbn. isbn=9780135957059"));

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_ISBN"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldGetBookByIdWhenRoleIsMember() throws Exception {
        // Given
        when(bookService.getById(BOOK_ID)).thenReturn(book());

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", BOOK_ID)
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.isbn").value("9780135957059"));
    }

    @Test
    void shouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
        // Given
        when(bookService.getById(BOOK_ID))
                .thenThrow(new ResourceNotFoundException("Book not found: " + BOOK_ID));

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", BOOK_ID)
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    private CreateBookRequest createBookRequest() {
        return new CreateBookRequest("The Pragmatic Programmer", "9780135957059", 2);
    }

    private Book book() {
        return new Book(BOOK_ID, "The Pragmatic Programmer", "9780135957059", 2, CREATED_AT, CREATED_AT);
    }

}
