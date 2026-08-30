package com.eyup.library.service;

import com.eyup.library.base.AbstractBaseServiceTest;
import com.eyup.library.domain.Loan;
import com.eyup.library.domain.LoanStatus;
import com.eyup.library.dto.CreateLoanRequest;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.entity.LoanEntity;
import com.eyup.library.exception.BusinessException;
import com.eyup.library.exception.DuplicateActiveLoanException;
import com.eyup.library.exception.NoAvailableCopiesException;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.mapper.LoanMapper;
import com.eyup.library.repository.BookRepository;
import com.eyup.library.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanServiceTest extends AbstractBaseServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LOAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDate DUE_DATE = LocalDate.parse("2099-01-15");

    private LoanService loanService;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    private final LoanMapper loanMapper = new LoanMapper();

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookRepository, loanMapper);
    }

    @Test
    void shouldCreateLoan() {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookEntity()));
        when(loanRepository.countByBookIdAndStatusNot(BOOK_ID, LoanStatus.RETURNED)).thenReturn(1L);
        when(loanRepository.existsByMemberIdAndBookIdAndStatus(MEMBER_ID, BOOK_ID, LoanStatus.ACTIVE))
                .thenReturn(false);
        when(loanRepository.saveAndFlush(any(LoanEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Loan loan = loanService.create(request);

        // Then
        assertThat(loan.bookId()).isEqualTo(BOOK_ID);
        assertThat(loan.memberId()).isEqualTo(MEMBER_ID);
        assertThat(loan.dueDate()).isEqualTo(DUE_DATE);
        assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE);

        verify(loanRepository).saveAndFlush(argThat(argument ->
                argument.getBook().getId().equals(BOOK_ID)
                        && argument.getMemberId().equals(MEMBER_ID)
                        && argument.getDueDate().equals(DUE_DATE)
                        && argument.getStatus() == LoanStatus.ACTIVE
        ));
    }

    @Test
    void shouldThrowExceptionWhenCreatingLoanForMissingBook() {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");

        verify(loanRepository, never()).saveAndFlush(any(LoanEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenNoCopiesAvailable() {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookEntity()));
        when(loanRepository.countByBookIdAndStatusNot(BOOK_ID, LoanStatus.RETURNED)).thenReturn(2L);

        // When & Then
        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(NoAvailableCopiesException.class)
                .hasMessageContaining("No available copies");

        verify(loanRepository, never()).saveAndFlush(any(LoanEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenMemberAlreadyHasActiveLoanOfSameBook() {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookEntity()));
        when(loanRepository.countByBookIdAndStatusNot(BOOK_ID, LoanStatus.RETURNED)).thenReturn(1L);
        when(loanRepository.existsByMemberIdAndBookIdAndStatus(MEMBER_ID, BOOK_ID, LoanStatus.ACTIVE))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(DuplicateActiveLoanException.class)
                .hasMessageContaining("already has an active loan");

        verify(loanRepository, never()).saveAndFlush(any(LoanEntity.class));
    }

    @Test
    void shouldReturnLoan() {
        // Given
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loanEntity()));
        when(loanRepository.saveAndFlush(any(LoanEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Loan loan = loanService.returnLoan(LOAN_ID);

        // Then
        assertThat(loan.id()).isEqualTo(LOAN_ID);
        assertThat(loan.status()).isEqualTo(LoanStatus.RETURNED);

        verify(loanRepository).saveAndFlush(argThat(argument -> argument.getStatus() == LoanStatus.RETURNED));
    }

    @Test
    void shouldThrowExceptionWhenReturningAlreadyReturnedLoan() {
        // Given
        LoanEntity returnedLoan = loanEntity();
        returnedLoan.markReturned();

        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(returnedLoan));

        // When & Then
        assertThatThrownBy(() -> loanService.returnLoan(LOAN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already returned");

        verify(loanRepository, never()).saveAndFlush(any(LoanEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenReturningMissingLoan() {
        // Given
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.returnLoan(LOAN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");

        verify(loanRepository, never()).saveAndFlush(any(LoanEntity.class));
    }

    @Test
    void shouldGetLoanById() {
        // Given
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loanEntity()));

        // When
        Loan loan = loanService.getById(LOAN_ID);

        // Then
        assertThat(loan.id()).isEqualTo(LOAN_ID);
        assertThat(loan.bookId()).isEqualTo(BOOK_ID);
        assertThat(loan.bookTitle()).isEqualTo("The Pragmatic Programmer");
        assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void shouldThrowExceptionWhenLoanDoesNotExist() {
        // Given
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.getById(LOAN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");
    }

    @Test
    void shouldListLoansWithPaginationAndSorting() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(loanRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(loanEntity()), pageable, 1));

        // When
        Page<Loan> loans = loanService.getAll(pageable);

        // Then
        assertThat(loans.getContent()).hasSize(1);
        assertThat(loans.getContent().getFirst().id()).isEqualTo(LOAN_ID);
        assertThat(loans.getTotalElements()).isEqualTo(1);
        assertThat(loans.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private CreateLoanRequest createLoanRequest() {
        return new CreateLoanRequest(BOOK_ID, MEMBER_ID, DUE_DATE);
    }

    private BookEntity bookEntity() {
        return new BookEntity(BOOK_ID, "The Pragmatic Programmer", "9780135957059", 2);
    }

    private LoanEntity loanEntity() {
        return new LoanEntity(LOAN_ID, bookEntity(), MEMBER_ID, DUE_DATE);
    }

}
