package com.eyup.library.exception;

/**
 * Thrown when a loan is requested for a book whose copies are all out on loan.
 *
 * <p>Callers should treat this as a client error: the request is well-formed but
 * conflicts with the current availability of the book.</p>
 */
public class NoAvailableCopiesException extends BusinessException {

    public NoAvailableCopiesException(String message) {
        super(ErrorCode.NO_AVAILABLE_COPIES, message);
    }

}
