package com.example.tm.shared.exception;

/**
 * Represents an application exception for reference validation exception.
 */
public class ReferenceValidationException extends RuntimeException {

    // Creates a new instance of reference validation exception.
    public ReferenceValidationException(String message) {
        super(message);
    }
}
