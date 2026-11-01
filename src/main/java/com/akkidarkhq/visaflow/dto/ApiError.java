package com.akkidarkhq.visaflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String code;         // e.g. "RESOURCE_NOT_FOUND", "VALIDATION_FAILED"
    private int status;          // HTTP Status code: 400, 404, 500
    private String details;      // Human-readable message or explanation
    private String path;         // Request URI (e.g. /api/visa-applications)
    private List<FieldErrorDetail> fieldErrors; // Optional list for 400 validation failures

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private String field;
        private Object rejectedValue;
        private String message;
    }

    public static ApiError of(String code, int status, String details) {
        return ApiError.builder()
                .code(code)
                .status(status)
                .details(details)
                .build();
    }

    public static ApiError of(String code, int status, String details, String path) {
        return ApiError.builder()
                .code(code)
                .status(status)
                .details(details)
                .path(path)
                .build();
    }
}
