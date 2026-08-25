package com.F_A_T.F_A_T.domain.project.repository;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
