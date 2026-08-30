package com.eyup.library.exception;

/**
 * Thrown when a member who already holds an active loan of a book requests
 * another loan of the same book.
 *
 * <p>Callers should treat this as a client error: the request is well-formed but
 * conflicts with existing state.</p>
 */
public class DuplicateActiveLoanException extends BusinessException {

    public DuplicateActiveLoanException(String message) {
        super(ErrorCode.DUPLICATE_ACTIVE_LOAN, message);
    }

}
