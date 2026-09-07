package com.F_A_T.F_A_T.domain.feedback.repository;

import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f WHERE f.project.project_id = :projectId")
    List<Feedback> findByProjectId(@Param("projectId") Long projectId);
}