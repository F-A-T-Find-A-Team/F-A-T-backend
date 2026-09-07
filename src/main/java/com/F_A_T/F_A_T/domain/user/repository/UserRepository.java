package com.F_A_T.F_A_T.domain.user.repository;

import com.F_A_T.F_A_T.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.user_email = :email")
    Optional<User> findByUserEmail(String userEmail); // 이메일로 찾기

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.user_email = :email")
    boolean existsByUserEmail(String userEmail);
}
