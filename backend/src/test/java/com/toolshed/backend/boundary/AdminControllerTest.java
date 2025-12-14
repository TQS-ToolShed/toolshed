package com.toolshed.backend.boundary;

import com.toolshed.backend.dto.AdminStatsDTO;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AdminService adminService;

        @Test
        @DisplayName("Should return admin stats")
        void getStats() throws Exception {
                AdminStatsDTO stats = AdminStatsDTO.builder()
                                .totalUsers(10)
                                .activeUsers(8)
                                .inactiveUsers(2)
                                .totalBookings(5)
                                .activeBookings(2)
                                .completedBookings(3)
                                .cancelledBookings(1)
                                .build();

                when(adminService.getStats()).thenReturn(stats);

                mockMvc.perform(get("/api/admin/stats")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalUsers").value(10))
                                .andExpect(jsonPath("$.activeUsers").value(8))
                                .andExpect(jsonPath("$.inactiveUsers").value(2))
                                .andExpect(jsonPath("$.totalBookings").value(5))
                                .andExpect(jsonPath("$.activeBookings").value(2))
                                .andExpect(jsonPath("$.cancelledBookings").value(1));
        }

        @Test
        @DisplayName("Should return all users")
        void getAllUsers() throws Exception {
                User user = User.builder()
                                .id(UUID.randomUUID())
                                .email("test@example.com")
                                .role(UserRole.RENTER)
                                .status(UserStatus.ACTIVE)
                                .build();

                when(adminService.getAllUsers()).thenReturn(Collections.singletonList(user));

                mockMvc.perform(get("/api/admin/users")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should activate user")
        void activateUser() throws Exception {
                UUID userId = UUID.randomUUID();
                User user = User.builder()
                                .id(userId)
                                .status(UserStatus.ACTIVE)
                                .build();

                when(adminService.activateUser(userId)).thenReturn(user);

                mockMvc.perform(post("/api/admin/users/{id}/activate", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should deactivate user")
        void deactivateUser() throws Exception {
                UUID userId = UUID.randomUUID();
                User user = User.builder()
                                .id(userId)
                                .status(UserStatus.SUSPENDED)
                                .build();

                when(adminService.deactivateUser(userId)).thenReturn(user);

                mockMvc.perform(post("/api/admin/users/{id}/deactivate", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }
}
