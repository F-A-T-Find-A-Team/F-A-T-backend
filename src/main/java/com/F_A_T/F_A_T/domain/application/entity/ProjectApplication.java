package com.F_A_T.F_A_T.domain.application.entity;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_application")
@Getter
@NoArgsConstructor
public class ProjectApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long application_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(name = "application_message")
    private String application_message;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false)
    private ApplicationStatus application_status;

    @Builder
    public ProjectApplication(Project project, User applicant, String application_message) {
        this.project = project;
        this.applicant = applicant;
        this.application_message = application_message;
        this.application_status = ApplicationStatus.PENDING;
    }

    public void changeStatus(ApplicationStatus newStatus) {
        this.application_status = newStatus;
    }
}