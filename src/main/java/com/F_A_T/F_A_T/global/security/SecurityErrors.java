package com.F_A_T.F_A_T.global.security;

import com.F_A_T.F_A_T.global.exception.ErrorCode;
import com.F_A_T.F_A_T.global.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

@Component
public class SecurityErrors {
    private final ObjectMapper mapper;
    public SecurityErrors(ObjectMapper mapper) { this.mapper=mapper; }
    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getOutputStream(),ApiErrorResponse.of(code));
    }
}
