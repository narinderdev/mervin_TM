package com.example.tm.shared.web;

import com.example.tm.shared.constants.HeaderConstants;
import com.example.tm.shared.exception.ReferenceValidationException;
import com.example.tm.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Encapsulates global exception handler functionality.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Handles handle reference validation. */
    @ExceptionHandler(ReferenceValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleReferenceValidation(
            ReferenceValidationException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    /** Handles handle resource not found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    /** Handles handle response status exception. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return buildErrorResponse(HttpStatus.valueOf(exception.getStatusCode().value()), message, request);
    }

    /** Handles handle method argument not valid. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldMessage)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Handles malformed request payload. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

    /** Handles missing request parameter. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Missing required request parameter: " + exception.getParameterName(), request);
    }

    /** Handles invalid request parameter type conversion. */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(
            TypeMismatchException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameter value", request);
    }

    /** Handles handle no resource found. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    /** Handles handle database exception. */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ApiErrorResponse> handleDatabaseException(
            Exception exception,
            HttpServletRequest request) {
        log.error("Database exception on {} {} cid={} msg={}",
                request.getMethod(),
                request.getRequestURI(),
                extractCorrelationId(request),
                exception.getMessage(),
                exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed", request);
    }

    /** Handles handle generic exception. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal server error",
                request
        );
    }

    /** Builds error response. */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(status).body(body);
    }

    /** Handles extract correlation id. */
    private String extractCorrelationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(HeaderConstants.CORRELATION_ID_HEADER);
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }

        String fromHeader = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return fromHeader == null ? "" : fromHeader;
    }

    /** Converts data to field message. */
    private String toFieldMessage(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage() == null
                ? "is invalid"
                : fieldError.getDefaultMessage();
        return fieldError.getField() + " " + defaultMessage;
    }
}
