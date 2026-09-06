package com.F_A_T.F_A_T.domain.feedback.repository;

import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByProject_Project_id(Long projectId);
}