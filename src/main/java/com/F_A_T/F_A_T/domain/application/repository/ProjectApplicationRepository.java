package com.F_A_T.F_A_T.domain.application.repository;

import com.F_A_T.F_A_T.domain.application.entity.ApplicationStatus;
import com.F_A_T.F_A_T.domain.application.entity.ProjectApplication;
import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM ProjectApplication a WHERE a.project = :project AND a.applicant = :applicant")
    boolean existsByProjectAndApplicant(@Param("project") Project project, @Param("applicant") User applicant);

    @Query("SELECT COUNT(a) FROM ProjectApplication a WHERE a.project = :project AND a.application_status = :status")
    int countByProjectAndStatus(@Param("project") Project project, @Param("status") ApplicationStatus status);

    @Query("SELECT a.applicant FROM ProjectApplication a WHERE a.project = :project AND a.application_status = :status")
    List<User> findAcceptedApplicants(@Param("project") Project project, @Param("status") ApplicationStatus status);
}