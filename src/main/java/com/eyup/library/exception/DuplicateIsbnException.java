package com.eyup.library.exception;

/**
 * Thrown when a book is created with an ISBN that is already registered.
 *
 * <p>Callers should treat this as a client error: the request is well-formed but
 * conflicts with existing state.</p>
 */
public class DuplicateIsbnException extends BusinessException {

    public DuplicateIsbnException(String message) {
        super(ErrorCode.DUPLICATE_ISBN, message);
    }

}
