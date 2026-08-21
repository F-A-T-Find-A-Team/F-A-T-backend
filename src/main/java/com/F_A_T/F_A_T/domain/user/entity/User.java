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


    //이줄 아래부터 다시 공부해볼것
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_interest_stack",
            joinColumns = @JoinColumn(name = "user_id")
    )


    @Column(name = "interest_stack")
    private List<String> interestStacks = new ArrayList<>();

    @Column(name = "user_student_number", nullable = false)
    private Integer userStudentNumber;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Builder
    public User(String userPassword, String userEmail, String userGender, String userMajor, List<String> interestStacks, Integer userStudentNumber, String userName) {
        this.user_password = userPassword;
        this.user_email = userEmail;
        this.user_gender = userGender;
        this.user_major = userMajor;
        this.interestStacks = interestStacks != null ? interestStacks : new ArrayList<>();
        this.userStudentNumber = userStudentNumber;
        this.userName = userName;
    }
}