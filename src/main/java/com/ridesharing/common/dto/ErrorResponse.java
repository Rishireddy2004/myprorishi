package com.ridesharing.common.dto;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final ErrorDetail error;

    public ErrorResponse(String code, String message) {
        this.error = new ErrorDetail(code, message);
    }
}
