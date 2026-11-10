package com.F_A_T.F_A_T.global.exception;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    public ApiException(ErrorCode errorCode) { super(errorCode.getMessage()); this.errorCode = errorCode; }
    public ApiException(ErrorCode errorCode, String message) { super(message); this.errorCode=errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
}
