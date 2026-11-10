package com.F_A_T.F_A_T.domain.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
public record ApplicationCreateRequest(@JsonAlias("message") String reason, String position) {
    public String message() { return reason; }
}
