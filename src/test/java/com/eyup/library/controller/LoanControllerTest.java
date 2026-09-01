package com.eyup.library.controller;

import com.eyup.library.base.AbstractRestControllerTest;
import com.eyup.library.domain.Loan;
import com.eyup.library.domain.LoanStatus;
import com.eyup.library.dto.CreateLoanRequest;
import com.eyup.library.exception.BusinessException;
import com.eyup.library.exception.DuplicateActiveLoanException;
import com.eyup.library.exception.ErrorCode;
import com.eyup.library.exception.NoAvailableCopiesException;
import com.eyup.library.exception.ResourceNotFoundException;
import com.eyup.library.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerTest extends AbstractRestControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LOAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDate DUE_DATE = LocalDate.parse("2099-01-15");
    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @MockitoBean
    private LoanService loanService;

    @Test
    void shouldCreateLoanWhenRoleIsLibrarian() throws Exception {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(loanService.create(any(CreateLoanRequest.class))).thenReturn(loan(LoanStatus.ACTIVE));

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(LOAN_ID.toString()))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(loanService).create(argThat(argument ->
                argument.bookId().equals(BOOK_ID)
                        && argument.memberId().equals(MEMBER_ID)
                        && argument.dueDate().equals(DUE_DATE)
        ));
    }

    @Test
    void shouldRejectCreateLoanWhenRoleIsMember() throws Exception {
        // Given
        CreateLoanRequest request = createLoanRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .with(user("member").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldRejectCreateLoanWhenUserIsNotAuthenticated() throws Exception {
        // Given
        CreateLoanRequest request = createLoanRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturnValidationErrorWhenCreateLoanRequestIsInvalid() throws Exception {
        // Given
        CreateLoanRequest request = new CreateLoanRequest(null, null, null);

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.bookId").exists())
                .andExpect(jsonPath("$.errors.memberId").exists())
                .andExpect(jsonPath("$.errors.dueDate").exists());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturnConflictWhenNoCopiesAvailable() throws Exception {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(loanService.create(any(CreateLoanRequest.class)))
                .thenThrow(new NoAvailableCopiesException("No available copies for book. bookId=" + BOOK_ID));

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_COPIES"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnConflictWhenMemberAlreadyHasActiveLoanOfSameBook() throws Exception {
        // Given
        CreateLoanRequest request = createLoanRequest();

        when(loanService.create(any(CreateLoanRequest.class)))
                .thenThrow(new DuplicateActiveLoanException(
                        "Member already has an active loan for this book. memberId=" + MEMBER_ID));

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACTIVE_LOAN"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnLoanWhenRoleIsLibrarian() throws Exception {
        // Given
        when(loanService.returnLoan(LOAN_ID)).thenReturn(loan(LoanStatus.RETURNED));

        // When & Then
        mockMvc.perform(patch("/api/v1/loans/{id}/return", LOAN_ID)
                        .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOAN_ID.toString()))
                .andExpect(jsonPath("$.status").value("RETURNED"));

        verify(loanService).returnLoan(LOAN_ID);
    }

    @Test
    void shouldRejectReturnLoanWhenRoleIsMember() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/v1/loans/{id}/return", LOAN_ID)
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturnConflictWhenReturningAlreadyReturnedLoan() throws Exception {
        // Given
        when(loanService.returnLoan(LOAN_ID))
                .thenThrow(new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Loan is already returned. loanId=" + LOAN_ID));

        // When & Then
        mockMvc.perform(patch("/api/v1/loans/{id}/return", LOAN_ID)
                        .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldGetLoanByIdWhenRoleIsMember() throws Exception {
        // Given
        when(loanService.getById(LOAN_ID)).thenReturn(loan(LoanStatus.ACTIVE));

        // When & Then
        mockMvc.perform(get("/api/v1/loans/{id}", LOAN_ID)
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOAN_ID.toString()))
                .andExpect(jsonPath("$.bookTitle").value("The Pragmatic Programmer"))
                .andExpect(jsonPath("$.dueDate").value(DUE_DATE.toString()));
    }

    @Test
    void shouldReturnNotFoundWhenLoanDoesNotExist() throws Exception {
        // Given
        when(loanService.getById(LOAN_ID))
                .thenThrow(new ResourceNotFoundException("Loan not found: " + LOAN_ID));

        // When & Then
        mockMvc.perform(get("/api/v1/loans/{id}", LOAN_ID)
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldListLoansWithPaginationAndSorting() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dueDate"));

        when(loanService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan(LoanStatus.ACTIVE)), pageable, 1));

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "dueDate,desc")
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(LOAN_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.sort[0]").value("dueDate,desc"));

        verify(loanService).getAll(argThat(argument ->
                argument.getPageNumber() == 0
                        && argument.getPageSize() == 5
                        && argument.getSort().getOrderFor("dueDate").getDirection() == Sort.Direction.DESC
        ));
    }

    private CreateLoanRequest createLoanRequest() {
        return new CreateLoanRequest(BOOK_ID, MEMBER_ID, DUE_DATE);
    }

    private Loan loan(LoanStatus status) {
        return new Loan(
                LOAN_ID,
                BOOK_ID,
                "The Pragmatic Programmer",
                MEMBER_ID,
                DUE_DATE,
                status,
                CREATED_AT,
                CREATED_AT
        );
    }

}
