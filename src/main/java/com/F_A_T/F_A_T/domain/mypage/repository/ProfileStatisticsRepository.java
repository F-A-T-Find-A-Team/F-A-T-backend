package com.F_A_T.F_A_T.domain.mypage.repository;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileStatisticsRepository {
    private final EntityManager entityManager;

    public long projectCount(Long userId, ProjectStatus stage) {
        return entityManager.createQuery("select count(m) from ProjectMember m where m.user.user_id = :id and m.project.project_status = :stage", Long.class)
                .setParameter("id", userId).setParameter("stage", stage).getSingleResult();
    }
    public long feedbackCount(Long userId) {
        return entityManager.createQuery("select count(f) from Feedback f where f.target.user_id = :id", Long.class)
                .setParameter("id", userId).getSingleResult();
    }
    public double averageRating(Long userId) {
        Double average = entityManager.createQuery("select avg(f.rating) from Feedback f where f.target.user_id = :id", Double.class)
                .setParameter("id", userId).getSingleResult();
        return average == null ? 0.0 : average;
    }
    public long memberCount(Long projectId) {
        return entityManager.createQuery("select count(m) from ProjectMember m where m.project.project_id = :id", Long.class)
                .setParameter("id", projectId).getSingleResult();
    }
    public Page<Project> history(Long userId, ProjectStatus stage, int page, int size) {
        String filter = " from ProjectMember m where m.user.user_id = :id" +
                (stage == null ? "" : " and m.project.project_status = :stage");
        var items = entityManager.createQuery("select m.project" + filter + " order by m.project.project_id desc", Project.class)
                .setParameter("id", userId).setFirstResult(page * size).setMaxResults(size);
        var count = entityManager.createQuery("select count(m)" + filter, Long.class).setParameter("id", userId);
        if (stage != null) { items.setParameter("stage", stage); count.setParameter("stage", stage); }
        return new PageImpl<>(items.getResultList(), PageRequest.of(page, size), count.getSingleResult());
    }
}
