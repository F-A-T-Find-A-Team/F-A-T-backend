package com.F_A_T.F_A_T.domain.project.entity;

import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long project_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_id", nullable = false)
    private User pm;

    @Column(name = "project_title", nullable = false)
    private String project_title;

    @Column(name = "project_description", columnDefinition = "TEXT", nullable = false)
    private String project_description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_required_major",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "required_major")
    private List<String> required_majors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_required_stack",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "required_stack")
    private List<String> required_stacks = new ArrayList<>();

    @Column(name = "project_deadline", nullable = false)
    private LocalDate project_deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false)
    private ProjectStatus project_status;

    @Builder
    public Project(User pm, String project_title, String project_description,
                   List<String> required_majors, List<String> required_stacks,
                   LocalDate project_deadline) {
        this.pm = pm;
        this.project_title = project_title;
        this.project_description = project_description;
        this.required_majors = required_majors != null ? required_majors : new ArrayList<>();
        this.required_stacks = required_stacks != null ? required_stacks : new ArrayList<>();
        this.project_deadline = project_deadline;
        this.project_status = ProjectStatus.IDEA; // 생성 시 기본값: 1단계(아이디어)
    }

    public void changeStatus(ProjectStatus newStatus) {
        this.project_status = newStatus;
    }
}
