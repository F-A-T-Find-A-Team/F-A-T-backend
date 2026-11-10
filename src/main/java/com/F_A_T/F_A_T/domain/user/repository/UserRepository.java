package com.F_A_T.F_A_T.domain.user.repository;

import com.F_A_T.F_A_T.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.user_email = :email")
    Optional<User> findByUserEmail(@Param("email") String userEmail); // 이메일로 찾기

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.user_email = :email")
    boolean existsByUserEmail(@Param("email") String userEmail);

    @Query("select count(u) > 0 from User u where (u.nickname = :nickname or (u.nickname is null and u.user_name = :nickname)) and (:excludeId is null or u.user_id <> :excludeId)")
    boolean nicknameExists(@Param("nickname") String nickname, @Param("excludeId") Long excludeId);
}
