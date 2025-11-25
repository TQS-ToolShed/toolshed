package com.toolshed.backend.repository.entities;

import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user") 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private Double reputationScore;

    @CreationTimestamp
    private LocalDateTime registeredDate;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getRegisteredDate() { return registeredDate; }
    public void setRegisteredDate(LocalDateTime registeredDate) { this.registeredDate = registeredDate; }
}