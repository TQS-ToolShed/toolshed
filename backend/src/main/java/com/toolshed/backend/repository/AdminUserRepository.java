package com.toolshed.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toolshed.backend.repository.entities.AdminUser;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    
    Optional<AdminUser> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
