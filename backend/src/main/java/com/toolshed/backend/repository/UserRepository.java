package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    long countByStatus(com.toolshed.backend.repository.enums.UserStatus status);
}
