package com.F_A_T.F_A_T.domain.project.entity;

import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_members", uniqueConstraints = @UniqueConstraint(name = "uk_project_member", columnNames = {"project_id", "user_id"}))
@Getter
@NoArgsConstructor
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ProjectMember(Project project, User user) {
        this.project = project;
        this.user = user;
    }
}
