package com.F_A_T.F_A_T.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long user_id;

    @Column(name = "user_password", nullable = false)
    private String user_password;

    @Column(name = "user_email", nullable = false, unique = true)
    private String user_email;

    @Column(name = "user_gender", nullable = false)
    private String user_gender;

    @Column(name = "user_major", nullable = false)
    private String user_major;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_interest_stack",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "interest_stack")
    private List<String> interest_stacks = new ArrayList<>();

    @Column(name = "user_student_number", nullable = false)
    private Integer user_student_number;

    @Column(name = "user_name", nullable = false)
    private String user_name;

    @Column(name = "github_username")
    private String github_username;

    @Builder
    public User(String user_password, String user_email, String user_gender,
                String user_major, List<String> interest_stacks,
                Integer user_student_number, String user_name) {
        this.user_password = user_password;
        this.user_email = user_email;
        this.user_gender = user_gender;
        this.user_major = user_major;
        this.interest_stacks = interest_stacks != null ? interest_stacks : new ArrayList<>();
        this.user_student_number = user_student_number;
        this.user_name = user_name;
    }
}