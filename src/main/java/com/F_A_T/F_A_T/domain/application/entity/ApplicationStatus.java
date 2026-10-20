package com.F_A_T.F_A_T.domain.application.entity;

public enum ApplicationStatus {
    PENDING,    // 대기중
    ACCEPTED,   // 수락됨
    REJECTED,   // 거절됨
    CANCELLED   // 본인 지원 취소; 이력 보존
}