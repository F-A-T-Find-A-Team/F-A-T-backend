package com.F_A_T.F_A_T.domain.project.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDate;
import java.util.List;

public record ProjectCreateRequest(
        @JsonAlias("projectTitle") String title,
        @JsonAlias("projectDescription") String description,
        List<String> requiredMajors,
        @JsonAlias("requiredStacks") List<String> techStacks,
        @JsonAlias("projectDeadline") LocalDate deadline,
        @JsonAlias("recruitLimit") Integer capacity
) {
    public String projectTitle() { return title; }
    public String projectDescription() { return description; }
    public List<String> requiredStacks() { return techStacks; }
    public LocalDate projectDeadline() { return deadline; }
    public Integer recruitLimit() { return capacity; }
}
