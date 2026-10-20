package com.F_A_T.F_A_T.domain.project.repository;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.project.entity.ProjectMember;
import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    @Query("select count(m) > 0 from ProjectMember m where m.project.project_id = :projectId and m.user.user_id = :userId")
    boolean existsMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("select m.user from ProjectMember m where m.project.project_id = :projectId order by m.id")
    List<User> findMembers(@Param("projectId") Long projectId);

    @Query("select count(m) from ProjectMember m where m.project.project_id = :projectId")
    int countMembers(@Param("projectId") Long projectId);

    @Query("select m.project from ProjectMember m where m.user.user_id = :userId order by m.project.project_id desc")
    List<Project> findProjects(@Param("userId") Long userId);
}
