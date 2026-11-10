package com.F_A_T.F_A_T.global.dto;

import com.F_A_T.F_A_T.global.exception.ErrorCode;
public record ApiErrorResponse(boolean success, Detail error) {
    public record Detail(String code, String message) {}
    public static ApiErrorResponse of(ErrorCode code,String message) { return new ApiErrorResponse(false,new Detail(code.name(),message)); }
    public static ApiErrorResponse of(ErrorCode code) {
        return new ApiErrorResponse(false, new Detail(code.name(), code.getMessage()));
    }
}
