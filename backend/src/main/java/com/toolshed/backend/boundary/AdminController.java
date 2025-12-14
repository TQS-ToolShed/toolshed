package com.toolshed.backend.boundary;

import com.toolshed.backend.dto.AdminStatsDTO;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<User> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.activateUser(id));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<User> deactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deactivateUser(id));
    }
}
