package com.ridesharing.common.exception;

public class ExternalServiceException extends RuntimeException {
    private final String code;

    public ExternalServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ExternalServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
