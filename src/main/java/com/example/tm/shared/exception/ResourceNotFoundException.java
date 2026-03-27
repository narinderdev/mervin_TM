package com.example.tm.shared.exception;

/**
 * Represents an application exception for resource not found exception.
 */
public class ResourceNotFoundException extends RuntimeException {

    /** Creates a new instance of resource not found exception. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
