package com.EchoLink.auth_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.EchoLink.auth_server.model.User;

/**
 * User 데이터를 데이터베이스에서 조회, 저장, 삭제하는 기능을 담당할 인터페이스
 */
public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일을 기준으로 사용자를 찾는 메소드
    Optional<User> findByEmail(String email);
}