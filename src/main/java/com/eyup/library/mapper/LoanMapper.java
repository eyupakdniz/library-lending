package com.eyup.library.mapper;

import com.eyup.library.domain.Loan;
import com.eyup.library.dto.LoanResponse;
import com.eyup.library.entity.LoanEntity;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public Loan toDomain(LoanEntity entity) {
        return new Loan(
                entity.getId(),
                entity.getBook().getId(),
                entity.getBook().getTitle(),
                entity.getMemberId(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.id(),
                loan.bookId(),
                loan.bookTitle(),
                loan.memberId(),
                loan.dueDate(),
                loan.status(),
                loan.createdAt(),
                loan.updatedAt()
        );
    }

}
