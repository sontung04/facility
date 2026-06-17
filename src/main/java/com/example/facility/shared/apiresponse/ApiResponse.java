package com.example.facility.shared.apiresponse;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record ApiResponse<T>(
        int code,
        String message,
        T data) {
    @Getter
    @AllArgsConstructor
    private enum SuccessCode {
        SUCCESS(1000, "Request success.", HttpStatus.OK);

        private final int code;
        private final String successMessage;
        private final HttpStatus httpStatus;
    }

    public ApiResponse(T data) {
        this(
                SuccessCode.SUCCESS.code,
                SuccessCode.SUCCESS.successMessage,
                data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(SuccessCode.SUCCESS.code, message, data);
    }
}

