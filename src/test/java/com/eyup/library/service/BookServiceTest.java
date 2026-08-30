package com.eyup.library.service;

import com.eyup.library.base.AbstractBaseServiceTest;
import com.eyup.library.domain.Book;
import com.eyup.library.dto.CreateBookRequest;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.mapper.BookMapper;
import com.eyup.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest extends AbstractBaseServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private BookService bookService;

    @Mock
    private BookRepository bookRepository;

    private final BookMapper bookMapper = new BookMapper();

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookMapper);
    }

    @Test
    void shouldCreateBook() {
        // Given
        CreateBookRequest request = createBookRequest();

        when(bookRepository.saveAndFlush(any(BookEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Book book = bookService.create(request);

        // Then
        assertThat(book.title()).isEqualTo("The Pragmatic Programmer");
        assertThat(book.isbn()).isEqualTo("9780135957059");
        assertThat(book.copies()).isEqualTo(2);

        verify(bookRepository).saveAndFlush(argThat(argument ->
                argument.getTitle().equals("The Pragmatic Programmer")
                        && argument.getIsbn().equals("9780135957059")
                        && argument.getCopies() == 2
        ));
    }

    @Test
    void shouldGetBookById() {
        // Given
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookEntity()));

        // When
        Book book = bookService.getById(BOOK_ID);

        // Then
        assertThat(book.id()).isEqualTo(BOOK_ID);
        assertThat(book.title()).isEqualTo("The Pragmatic Programmer");
    }

    @Test
    void shouldThrowExceptionWhenBookDoesNotExist() {
        // Given
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.getById(BOOK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    void shouldListBooksWithPaginationAndSorting() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(bookRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(bookEntity()), pageable, 1));

        // When
        Page<Book> books = bookService.getAll(pageable);

        // Then
        assertThat(books.getContent()).hasSize(1);
        assertThat(books.getContent().getFirst().id()).isEqualTo(BOOK_ID);
        assertThat(books.getTotalElements()).isEqualTo(1);
    }

    private CreateBookRequest createBookRequest() {
        return new CreateBookRequest("The Pragmatic Programmer", "9780135957059", 2);
    }

    private BookEntity bookEntity() {
        return new BookEntity(BOOK_ID, "The Pragmatic Programmer", "9780135957059", 2);
    }

}
