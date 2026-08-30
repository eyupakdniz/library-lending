package com.eyup.library.controller;

import com.eyup.library.api.ApiResponse;
import com.eyup.library.dto.CreateLoanRequest;
import com.eyup.library.dto.LoanPageResponse;
import com.eyup.library.dto.LoanResponse;
import com.eyup.library.mapper.LoanMapper;
import com.eyup.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper loanMapper;

    public LoanController(LoanService loanService, LoanMapper loanMapper) {
        this.loanService = loanService;
        this.loanMapper = loanMapper;
    }

    @Operation(summary = "Create loan. ROLE_LIBRARIAN only.")
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoanResponse> create(@Valid @RequestBody CreateLoanRequest request) {
        return ApiResponse.success(loanMapper.toResponse(loanService.create(request)));
    }

    @Operation(summary = "Return loan. ROLE_LIBRARIAN only.")
    @PatchMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ApiResponse<LoanResponse> returnLoan(@PathVariable UUID id) {
        return ApiResponse.success(loanMapper.toResponse(loanService.returnLoan(id)));
    }

    @Operation(summary = "Get loan by id. Any authenticated user is allowed.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoanResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(loanMapper.toResponse(loanService.getById(id)));
    }

    @Operation(summary = "List loans. Any authenticated user is allowed.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoanPageResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                LoanPageResponse.from(loanService.getAll(pageable).map(loanMapper::toResponse)));
    }

}
