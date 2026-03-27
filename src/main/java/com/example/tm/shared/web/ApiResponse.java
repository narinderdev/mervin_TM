package com.example.tm.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Transfers api response data between layers.
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int statusCode;
    private String status;
    private String message;
    private T data;

    // Handles success response.
    public static <T> ApiResponse<T> successResponse(int statusCode, String message, T data) {
        return new ApiResponse<>(statusCode, "success", message, data);
    }

    // Handles error response.
    public static <T> ApiResponse<T> errorResponse(int statusCode, String message) {
        return new ApiResponse<>(statusCode, "error", message, null);
    }
}
