package com.F_A_T.F_A_T.domain.mypage.dto.response;

import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import java.util.List;

public record ProjectHistoryResponse(List<Item> items, int page, int size, long totalElements, int totalPages) {
    public record Item(Long id, String title, ProjectStatus stage, String role, long memberCount, int capacity) {}
}
