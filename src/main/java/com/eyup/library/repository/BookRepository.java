package com.eyup.library.repository;

import com.eyup.library.entity.BookEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<BookEntity, UUID> {

    boolean existsByIsbn(String isbn);

    /**
     * Reads the book row under a write lock so the availability count that guards
     * loan creation cannot change between the check and the insert.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from BookEntity book where book.id = :id")
    Optional<BookEntity> findByIdForUpdate(@Param("id") UUID id);

}
