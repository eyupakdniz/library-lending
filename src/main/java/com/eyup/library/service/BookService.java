package com.eyup.library.service;

import com.eyup.library.domain.Book;
import com.eyup.library.dto.CreateBookRequest;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.mapper.BookMapper;
import com.eyup.library.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional
    public Book create(CreateBookRequest request) {
        BookEntity entity = new BookEntity(UUID.randomUUID(), request.title(), request.isbn(), request.copies());
        return bookMapper.toDomain(bookRepository.saveAndFlush(entity));
    }

    @Transactional(readOnly = true)
    public Book getById(UUID id) {
        return bookMapper.toDomain(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<Book> getAll(Pageable pageable) {
        return bookRepository.findAll(pageable).map(bookMapper::toDomain);
    }

    private BookEntity findEntity(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

}
