package com.eyup.library.repository;

import com.eyup.library.base.AbstractDataJpaTest;
import com.eyup.library.domain.LoanStatus;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.entity.LoanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanRepositoryTest extends AbstractDataJpaTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_MEMBER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID LOAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDate DUE_DATE = LocalDate.parse("2099-01-15");

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    private BookEntity book;

    @BeforeEach
    void setUp() {
        book = bookRepository.saveAndFlush(new BookEntity(BOOK_ID, "The Pragmatic Programmer", "9780135957059", 2));
    }

    @Test
    void shouldCountActiveLoansOfBook() {
        // Given
        loanRepository.saveAndFlush(loanEntity(LOAN_ID, MEMBER_ID));
        loanRepository.saveAndFlush(returnedLoanEntity(UUID.randomUUID(), OTHER_MEMBER_ID));

        // When
        long activeLoans = loanRepository.countByBookIdAndStatus(BOOK_ID, LoanStatus.ACTIVE);

        // Then
        assertThat(activeLoans).isEqualTo(1);
    }

    @Test
    void shouldFindActiveLoanOfMemberAndBook() {
        // Given
        loanRepository.saveAndFlush(loanEntity(LOAN_ID, MEMBER_ID));

        // When
        boolean exists = loanRepository
                .existsByMemberIdAndBookIdAndStatus(MEMBER_ID, BOOK_ID, LoanStatus.ACTIVE);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotFindActiveLoanWhenLoanWasReturned() {
        // Given
        loanRepository.saveAndFlush(returnedLoanEntity(LOAN_ID, MEMBER_ID));

        // When
        boolean exists = loanRepository
                .existsByMemberIdAndBookIdAndStatus(MEMBER_ID, BOOK_ID, LoanStatus.ACTIVE);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldStoreStatusAsString() {
        // Given
        loanRepository.saveAndFlush(loanEntity(LOAN_ID, MEMBER_ID));

        // When
        LoanEntity found = loanRepository.findById(LOAN_ID).orElseThrow();

        // Then
        assertThat(found.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(found.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(found.getBook().getId()).isEqualTo(BOOK_ID);
    }

    private LoanEntity loanEntity(UUID id, UUID memberId) {
        return new LoanEntity(id, book, memberId, DUE_DATE);
    }

    private LoanEntity returnedLoanEntity(UUID id, UUID memberId) {
        LoanEntity loan = loanEntity(id, memberId);
        loan.markReturned();
        return loan;
    }

}
