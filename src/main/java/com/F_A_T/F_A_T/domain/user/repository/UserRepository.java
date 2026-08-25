package com.F_A_T.F_A_T.domain.user.repository;

import com.F_A_T.F_A_T.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserEmail(String userEmail); // 이메일로 찾기

    boolean existsByUserEmail(String userEmail);
}
