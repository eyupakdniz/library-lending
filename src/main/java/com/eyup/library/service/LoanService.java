package com.eyup.library.service;

import com.eyup.library.domain.Loan;
import com.eyup.library.domain.LoanStatus;
import com.eyup.library.dto.CreateLoanRequest;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.entity.LoanEntity;
import com.eyup.library.exception.BusinessException;
import com.eyup.library.exception.DuplicateActiveLoanException;
import com.eyup.library.exception.ErrorCode;
import com.eyup.library.exception.NoAvailableCopiesException;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.mapper.LoanMapper;
import com.eyup.library.repository.BookRepository;
import com.eyup.library.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final LoanMapper loanMapper;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.loanMapper = loanMapper;
    }

    @Transactional
    public Loan create(CreateLoanRequest request) {
        BookEntity book = findBookEntity(request.bookId());

        rejectIfNoAvailableCopies(book);
        rejectIfMemberAlreadyHasActiveLoan(request.memberId(), book.getId());

        LoanEntity loan = new LoanEntity(UUID.randomUUID(), book, request.memberId(), request.dueDate());
        return loanMapper.toDomain(loanRepository.saveAndFlush(loan));
    }

    @Transactional
    public Loan returnLoan(UUID id) {
        LoanEntity loan = findLoanEntity(id);

        rejectIfAlreadyReturned(loan);

        loan.markReturned();
        return loanMapper.toDomain(loanRepository.saveAndFlush(loan));
    }

    @Transactional(readOnly = true)
    public Loan getById(UUID id) {
        return loanMapper.toDomain(findLoanEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<Loan> getAll(Pageable pageable) {
        return loanRepository.findAll(pageable).map(loanMapper::toDomain);
    }

    private void rejectIfNoAvailableCopies(BookEntity book) {
        long copiesOnLoan = loanRepository.countByBookIdAndStatusNot(book.getId(), LoanStatus.RETURNED);
        if (copiesOnLoan >= book.getCopies()) {
            throw new NoAvailableCopiesException(noAvailableCopiesMessage(book));
        }
    }

    private void rejectIfMemberAlreadyHasActiveLoan(UUID memberId, UUID bookId) {
        if (loanRepository.existsByMemberIdAndBookIdAndStatus(memberId, bookId, LoanStatus.ACTIVE)) {
            throw new DuplicateActiveLoanException(duplicateActiveLoanMessage(memberId, bookId));
        }
    }

    private void rejectIfAlreadyReturned(LoanEntity loan) {
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Loan is already returned. loanId=%s".formatted(loan.getId()));
        }
    }

    private BookEntity findBookEntity(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

    private LoanEntity findLoanEntity(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + id));
    }

    private String noAvailableCopiesMessage(BookEntity book) {
        return "No available copies for book. bookId=%s, title=%s".formatted(book.getId(), book.getTitle());
    }

    private String duplicateActiveLoanMessage(UUID memberId, UUID bookId) {
        return "Member already has an active loan for this book. memberId=%s, bookId=%s"
                .formatted(memberId, bookId);
    }

}
