package com.aishop.commerce.repository;

import com.aishop.commerce.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCaseOrPhoneOrEmailIgnoreCase(String username, String phone, String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByPhone(String phone);
    boolean existsByEmailIgnoreCase(String email);
}
