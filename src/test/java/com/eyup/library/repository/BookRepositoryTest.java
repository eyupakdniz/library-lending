package com.eyup.library.repository;

import com.eyup.library.base.AbstractDataJpaTest;
import com.eyup.library.entity.BookEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookRepositoryTest extends AbstractDataJpaTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_BOOK_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldFindBookByIsbn() {
        // Given
        bookRepository.saveAndFlush(bookEntity(BOOK_ID, "9780135957059"));

        // When
        boolean exists = bookRepository.existsByIsbn("9780135957059");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotFindBookByUnknownIsbn() {
        // When
        boolean exists = bookRepository.existsByIsbn("9999999999999");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenInsertingDuplicateIsbn() {
        // Given
        bookRepository.saveAndFlush(bookEntity(BOOK_ID, "9780135957059"));

        // When & Then
        assertThatThrownBy(() -> bookRepository.saveAndFlush(bookEntity(OTHER_BOOK_ID, "9780135957059")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindBookByIdForUpdate() {
        // Given
        bookRepository.saveAndFlush(bookEntity(BOOK_ID, "9780135957059"));

        // When
        Optional<BookEntity> found = bookRepository.findByIdForUpdate(BOOK_ID);

        // Then
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getTitle()).isEqualTo("The Pragmatic Programmer");
    }

    @Test
    void shouldMaintainTimestampsAndVersionOnInsert() {
        // When
        BookEntity saved = bookRepository.saveAndFlush(bookEntity(BOOK_ID, "9780135957059"));

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    private BookEntity bookEntity(UUID id, String isbn) {
        return new BookEntity(id, "The Pragmatic Programmer", isbn, 2);
    }

}
