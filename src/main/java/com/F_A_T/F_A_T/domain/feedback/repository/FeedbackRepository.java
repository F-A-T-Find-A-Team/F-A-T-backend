package com.F_A_T.F_A_T.domain.feedback.repository;

import com.F_A_T.F_A_T.domain.feedback.entity.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f WHERE f.project.project_id = :projectId")
    List<Feedback> findByProjectId(@Param("projectId") Long projectId);

    @Query("select f from Feedback f where f.target.user_id = :userId order by f.feedback_id desc")
    List<Feedback> findReceived(@Param("userId") Long userId);

    @Query("select avg(f.rating) from Feedback f where f.target.user_id = :userId")
    Double averageRating(@Param("userId") Long userId);

    @Query("select distinct f.target.user_id from Feedback f where f.project.project_id = :projectId")
    List<Long> findEvaluatedMemberIds(@Param("projectId") Long projectId);
}
