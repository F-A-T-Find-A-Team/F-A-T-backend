package com.F_A_T.F_A_T.domain.feedback.entity;

import com.F_A_T.F_A_T.domain.project.entity.Project;
import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedback_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Column(name = "feedback_content", columnDefinition = "TEXT", nullable = false)
    private String feedback_content;

    @Builder
    public Feedback(Project project, User writer, User target, String feedback_content) {
        this.project = project;
        this.writer = writer;
        this.target = target;
        this.feedback_content = feedback_content;
    }
}