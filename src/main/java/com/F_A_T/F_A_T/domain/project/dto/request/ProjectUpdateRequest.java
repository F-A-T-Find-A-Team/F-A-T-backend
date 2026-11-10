package com.F_A_T.F_A_T.domain.project.dto.request;

import java.time.LocalDate;
import java.util.List;

public record ProjectUpdateRequest(String title, String description, List<String> requiredMajors,
                                   List<String> techStacks, LocalDate deadline, Integer capacity,
                                   String recruitmentStatus) {}
