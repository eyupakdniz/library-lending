package com.eyup.library.mapper;

import com.eyup.library.domain.Book;
import com.eyup.library.dto.BookResponse;
import com.eyup.library.entity.BookEntity;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book toDomain(BookEntity entity) {
        return new Book(
                entity.getId(),
                entity.getTitle(),
                entity.getIsbn(),
                entity.getCopies(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.id(),
                book.title(),
                book.isbn(),
                book.copies(),
                book.createdAt(),
                book.updatedAt()
        );
    }

}
