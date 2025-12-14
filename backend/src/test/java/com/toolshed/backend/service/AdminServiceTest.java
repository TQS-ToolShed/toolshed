package com.toolshed.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.AdminStatsDTO;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminService adminService;

    private User buildUser(UserRole role, UserStatus status) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail("test@example.com");
        u.setPassword("secret");
        u.setRole(role);
        u.setStatus(status);
        u.setReputationScore(0.0);
        return u;
    }

    @Test
    @DisplayName("getStats aggregates counts from repositories")
    void getStatsAggregatesCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(7L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(3L);
        when(bookingRepository.count()).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.APPROVED)).thenReturn(5L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(12L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(3L);

        AdminStatsDTO stats = adminService.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(10);
        assertThat(stats.getActiveUsers()).isEqualTo(7);
        assertThat(stats.getInactiveUsers()).isEqualTo(3);
        assertThat(stats.getTotalBookings()).isEqualTo(20);
        assertThat(stats.getActiveBookings()).isEqualTo(5);
        assertThat(stats.getCompletedBookings()).isEqualTo(12);
        assertThat(stats.getCancelledBookings()).isEqualTo(3);
    }

    @Test
    @DisplayName("getAllUsers filters out admins")
    void getAllUsersFiltersAdmins() {
        User admin = buildUser(UserRole.ADMIN, UserStatus.ACTIVE);
        User renter = buildUser(UserRole.RENTER, UserStatus.ACTIVE);
        User supplier = buildUser(UserRole.SUPPLIER, UserStatus.SUSPENDED);
        when(userRepository.findAll()).thenReturn(Arrays.asList(admin, renter, supplier));

        List<User> result = adminService.getAllUsers();

        assertThat(result).containsExactlyInAnyOrder(renter, supplier);
        assertThat(result).noneMatch(u -> u.getRole() == UserRole.ADMIN);
    }

    @Test
    @DisplayName("activateUser sets status ACTIVE and saves user")
    void activateUserChangesStatus() {
        User user = buildUser(UserRole.RENTER, UserStatus.SUSPENDED);
        UUID id = user.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminService.activateUser(id);

        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("deactivateUser sets status SUSPENDED and saves user")
    void deactivateUserChangesStatus() {
        User user = buildUser(UserRole.SUPPLIER, UserStatus.ACTIVE);
        UUID id = user.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminService.deactivateUser(id);

        assertThat(updated.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("activateUser throws when user not found")
    void activateUserNotFound() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.activateUser(missingId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);

        verify(userRepository, never()).save(any(User.class));
    }
}
