package com.eyup.library.repository;

import com.eyup.library.domain.LoanStatus;
import com.eyup.library.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {

    boolean existsByMemberIdAndBookIdAndStatus(UUID memberId, UUID bookId, LoanStatus status);

    long countByBookIdAndStatusNot(UUID bookId, LoanStatus status);

}
