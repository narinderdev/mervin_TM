package com.example.tm.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Transfers api response data between layers.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int statusCode;
    private String status;
    private String message;
    private T data;

    // Creates a new instance of api response.
    public ApiResponse(int statusCode, String status, String message, T data) {
        this.statusCode = statusCode;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Handles error response.
    public static <T> ApiResponse<T> errorResponse(int statusCode, String message) {
        return new ApiResponse<>(statusCode, "error", message, null);
    }

    // Handles success response.
    public static <T> ApiResponse<T> successResponse(int statusCode, String message, T data) {
        return new ApiResponse<>(statusCode, "success", message, data);
    }
}
