package com.eyup.library.service;

import com.eyup.library.base.AbstractDataJpaTest;
import com.eyup.library.domain.LoanStatus;
import com.eyup.library.dto.CreateLoanRequest;
import com.eyup.library.entity.BookEntity;
import com.eyup.library.exception.BusinessException;
import com.eyup.library.exception.NoAvailableCopiesException;
import com.eyup.library.mapper.LoanMapper;
import com.eyup.library.repository.BookRepository;
import com.eyup.library.repository.LoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the pessimistic lock in {@link LoanService#create} actually holds: two
 * requests racing for the last copy of a book must produce exactly one loan.
 *
 * <p>A {@code @DataJpaTest} rolls its transaction back by default, so the racing
 * threads would never see the fixture row — hence {@code NOT_SUPPORTED} and the
 * manual clean-up.</p>
 */
@Import({LoanService.class, LoanMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LoanServiceConcurrencyTest extends AbstractDataJpaTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate DUE_DATE = LocalDate.parse("2099-01-15");

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @AfterEach
    void cleanUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void shouldCreateOnlyOneLoanWhenTwoRequestsRaceForLastCopy() throws Exception {
        // Given
        bookRepository.saveAndFlush(new BookEntity(BOOK_ID, "The Pragmatic Programmer", "9780135957059", 1));

        int racers = 2;
        ExecutorService executor = Executors.newFixedThreadPool(racers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Exception>> outcomes = new ArrayList<>();

        // When
        for (int racer = 0; racer < racers; racer++) {
            UUID memberId = UUID.randomUUID();
            outcomes.add(executor.submit(() -> {
                start.await();
                try {
                    loanService.create(new CreateLoanRequest(BOOK_ID, memberId, DUE_DATE));
                    return null;
                } catch (BusinessException exception) {
                    return exception;
                }
            }));
        }
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Then
        List<Exception> failures = new ArrayList<>();
        for (Future<Exception> outcome : outcomes) {
            if (outcome.get() != null) {
                failures.add(outcome.get());
            }
        }

        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(NoAvailableCopiesException.class);
        assertThat(loanRepository.countByBookIdAndStatus(BOOK_ID, LoanStatus.ACTIVE)).isEqualTo(1);
    }

}
