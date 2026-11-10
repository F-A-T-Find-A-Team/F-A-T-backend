package com.F_A_T.F_A_T.global.exception;

import com.F_A_T.F_A_T.global.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ResponseEntity<ApiErrorResponse> error(ErrorCode code) {
        return ResponseEntity.status(code.getHttpStatus()).body(ApiErrorResponse.of(code));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> business(ApiException ex) { return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ApiErrorResponse.of(ex.getErrorCode(),ex.getMessage())); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> forbidden() { return error(ErrorCode.FORBIDDEN); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> uploadSize() { return error(ErrorCode.IMAGE_TOO_LARGE); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class})
    ResponseEntity<ApiErrorResponse> invalid(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ErrorCode code = path.equals("/auth/email/verify") ? ErrorCode.INVALID_VERIFICATION_CODE
            : path.equals("/auth/email/send") ? ErrorCode.INVALID_EMAIL_DOMAIN
            : path.equals("/auth/signup") ? ErrorCode.INVALID_SIGNUP_DATA
            : path.contains("/feedback") ? ErrorCode.INVALID_FEEDBACK_DATA
            : path.endsWith("/stage") ? ErrorCode.INVALID_STAGE
            : path.equals("/users/me") ? ErrorCode.INVALID_PROFILE_DATA
            : path.endsWith("/applications") ? ErrorCode.INVALID_APPLICATION_DATA
            : path.startsWith("/applications/") ? ErrorCode.INVALID_APPLICATION_STATUS
            : path.equals("/projects") || path.startsWith("/projects/") ? ErrorCode.INVALID_PROJECT_DATA
            : ErrorCode.INVALID_REQUEST;
        return error(code);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> wrongType(MethodArgumentTypeMismatchException ex) {
        String name=ex.getName();
        return error(name.equals("projectId") ? ErrorCode.INVALID_PROJECT_ID
                : name.equals("userId") ? ErrorCode.INVALID_USER_ID
                : name.equals("page") || name.equals("size") ? ErrorCode.INVALID_PAGINATION : ErrorCode.INVALID_REQUEST);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> constraint(DataIntegrityViolationException ex) {
        // Do not send SQL, constraint names, PII, or JDBC messages to the client.
        String detail=String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(java.util.Locale.ROOT);
        ErrorCode code=detail.contains("nickname") ? ErrorCode.NICKNAME_ALREADY_EXISTS
            : detail.contains("uk_user_email") ? ErrorCode.EMAIL_ALREADY_EXISTS
            : detail.contains("uk_application_project_applicant") ? ErrorCode.ALREADY_APPLIED
            : detail.contains("uk_feedback_project_writer_target") ? ErrorCode.ALREADY_REVIEWED
            : detail.contains("uk_message_client") ? ErrorCode.DUPLICATE_MESSAGE : ErrorCode.INVALID_REQUEST;
        return error(code);
    }
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ResponseEntity<ApiErrorResponse> missingFile() { return error(ErrorCode.INVALID_IMAGE_TYPE); }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiErrorResponse> notFound() {
        return ResponseEntity.status(404).body(new ApiErrorResponse(false,
                new ApiErrorResponse.Detail("NOT_FOUND", "요청한 API를 찾을 수 없습니다.")));
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> method() {
        return ResponseEntity.status(405).body(new ApiErrorResponse(false,
                new ApiErrorResponse.Detail("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다.")));
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> mediaType(HttpServletRequest request) {
        if (request.getRequestURI().endsWith("/avatar")) return error(ErrorCode.INVALID_IMAGE_TYPE);
        return ResponseEntity.status(415).body(ApiErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    ResponseEntity<ApiErrorResponse> malformedUpload() { return error(ErrorCode.INVALID_IMAGE_TYPE); }
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> missingParameter() { return error(ErrorCode.INVALID_REQUEST); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled API failure: {}", ex.getClass().getSimpleName());
        return error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
