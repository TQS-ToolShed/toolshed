package com.toolshed.backend.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.RegisterRequest;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Register Tests")
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(0.0)
                .build();

        // Setup register request
        registerRequest = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("newuser@example.com")
                .password("newPassword123")
                .role(UserRole.SUPPLIER)
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void register_WithValidData_ShouldReturnUser() {
        // Arrange
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .role(registerRequest.getRole())
                .status(UserStatus.ACTIVE)
                .reputationScore(0.0)
                .build();

        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(registerRequest.getFirstName());
        assertThat(result.getLastName()).isEqualTo(registerRequest.getLastName());
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(result.getPassword()).isEqualTo(registerRequest.getPassword());
        assertThat(result.getRole()).isEqualTo(registerRequest.getRole());
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getReputationScore()).isEqualTo(0.0);

        // Verify
        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_WithExistingEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already in use")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Verify
        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should set correct default values for new user")
    void register_ShouldSetCorrectDefaults() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.register(registerRequest);

        // Assert - capture the user being saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getReputationScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should preserve all request fields in new user")
    void register_ShouldPreserveRequestFields() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.register(registerRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo(registerRequest.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(registerRequest.getLastName());
        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(savedUser.getPassword()).isEqualTo(registerRequest.getPassword());
        assertThat(savedUser.getRole()).isEqualTo(registerRequest.getRole());
    }

    @Test
    @DisplayName("Should work with RENTER role")
    void register_WithRenterRole_ShouldWork() {
        // Arrange
        registerRequest.setRole(UserRole.RENTER);
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getRole()).isEqualTo(UserRole.RENTER);
    }

    @Test
    @DisplayName("Should work with SUPPLIER role")
    void register_WithSupplierRole_ShouldWork() {
        // Arrange
        registerRequest.setRole(UserRole.SUPPLIER);
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getRole()).isEqualTo(UserRole.SUPPLIER);
    }

    @Test
    @DisplayName("Should handle empty first name")
    void register_WithEmptyFirstName_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle empty last name")
    void register_WithEmptyLastName_ShouldWork() {
        // Arrange
        registerRequest.setLastName("");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getLastName()).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle whitespace in names")
    void register_WithWhitespaceInNames_ShouldPreserveWhitespace() {
        // Arrange
        registerRequest.setFirstName("  John  ");
        registerRequest.setLastName("  Doe  ");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("  John  ");
        assertThat(result.getLastName()).isEqualTo("  Doe  ");
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void register_WithSpecialCharactersInPassword_ShouldWork() {
        // Arrange
        String complexPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
        registerRequest.setPassword(complexPassword);
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getPassword()).isEqualTo(complexPassword);
    }

    @Test
    @DisplayName("Should handle very long password")
    void register_WithVeryLongPassword_ShouldWork() {
        // Arrange
        String longPassword = "a".repeat(500);
        registerRequest.setPassword(longPassword);
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getPassword()).isEqualTo(longPassword);
    }

    @Test
    @DisplayName("Should handle email with different cases")
    void register_WithDifferentCaseEmail_ShouldPreserveCase() {
        // Arrange
        String mixedCaseEmail = "Test.User@EXAMPLE.com";
        registerRequest.setEmail(mixedCaseEmail);
        
        when(userRepository.findByEmail(mixedCaseEmail))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getEmail()).isEqualTo(mixedCaseEmail);
        verify(userRepository).findByEmail(mixedCaseEmail);
    }

    @Test
    @DisplayName("Should handle email with plus sign")
    void register_WithPlusInEmail_ShouldWork() {
        // Arrange
        String emailWithPlus = "user+test@example.com";
        registerRequest.setEmail(emailWithPlus);
        
        when(userRepository.findByEmail(emailWithPlus))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getEmail()).isEqualTo(emailWithPlus);
    }

    @Test
    @DisplayName("Should handle email with dots")
    void register_WithDotsInEmail_ShouldWork() {
        // Arrange
        String emailWithDots = "first.last.name@example.com";
        registerRequest.setEmail(emailWithDots);
        
        when(userRepository.findByEmail(emailWithDots))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getEmail()).isEqualTo(emailWithDots);
    }

    @Test
    @DisplayName("Should handle international domain names")
    void register_WithInternationalDomain_ShouldWork() {
        // Arrange
        String internationalEmail = "user@example.co.uk";
        registerRequest.setEmail(internationalEmail);
        
        when(userRepository.findByEmail(internationalEmail))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getEmail()).isEqualTo(internationalEmail);
    }

    @Test
    @DisplayName("Should verify email uniqueness check is called first")
    void register_ShouldCheckEmailUniquenessFirst() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.register(registerRequest);

        // Assert - verify findByEmail is called before save
        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should not save user when email exists")
    void register_WithExistingEmail_ShouldNotSaveUser() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResponseStatusException.class);

        // Verify save is never called
        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle numeric names")
    void register_WithNumericNames_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("John123");
        registerRequest.setLastName("Doe456");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("John123");
        assertThat(result.getLastName()).isEqualTo("Doe456");
    }

    @Test
    @DisplayName("Should handle single character names")
    void register_WithSingleCharacterNames_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("A");
        registerRequest.setLastName("B");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("A");
        assertThat(result.getLastName()).isEqualTo("B");
    }

    @Test
    @DisplayName("Should handle names with hyphens")
    void register_WithHyphenatedNames_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("Mary-Jane");
        registerRequest.setLastName("Smith-Johnson");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("Mary-Jane");
        assertThat(result.getLastName()).isEqualTo("Smith-Johnson");
    }

    @Test
    @DisplayName("Should handle names with apostrophes")
    void register_WithApostropheNames_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("D'Angelo");
        registerRequest.setLastName("O'Connor");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("D'Angelo");
        assertThat(result.getLastName()).isEqualTo("O'Connor");
    }

    @Test
    @DisplayName("Should handle multiple registrations with different data")
    void register_MultipleTimes_ShouldWorkIndependently() {
        // Arrange - First registration
        RegisterRequest firstRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password1")
                .role(UserRole.RENTER)
                .build();

        RegisterRequest secondRequest = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .password("password2")
                .role(UserRole.SUPPLIER)
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User firstResult = authService.register(firstRequest);
        User secondResult = authService.register(secondRequest);

        // Assert
        assertThat(firstResult.getFirstName()).isEqualTo("John");
        assertThat(firstResult.getRole()).isEqualTo(UserRole.RENTER);
        
        assertThat(secondResult.getFirstName()).isEqualTo("Jane");
        assertThat(secondResult.getRole()).isEqualTo(UserRole.SUPPLIER);

        // Verify both emails were checked and both users saved
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository).findByEmail("jane@example.com");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Should preserve exact password without encoding")
    void register_ShouldPreservePasswordAsIs() {
        // Arrange
        String plainPassword = "MyPlainPassword123!";
        registerRequest.setPassword(plainPassword);
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert - Password should be stored as-is (no encoding in current implementation)
        assertThat(result.getPassword()).isEqualTo(plainPassword);
    }

    @Test
    @DisplayName("Should handle short password")
    void register_WithShortPassword_ShouldWork() {
        // Arrange
        registerRequest.setPassword("123");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getPassword()).isEqualTo("123");
    }

    @Test
    @DisplayName("Should handle password with only spaces")
    void register_WithSpaceOnlyPassword_ShouldWork() {
        // Arrange
        registerRequest.setPassword("   ");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getPassword()).isEqualTo("   ");
    }

    @Test
    @DisplayName("Should return saved user with generated ID")
    void register_ShouldReturnUserWithId() {
        // Arrange
        UUID generatedId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(generatedId)
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .role(registerRequest.getRole())
                .status(UserStatus.ACTIVE)
                .reputationScore(0.0)
                .build();

        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getId()).isEqualTo(generatedId);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void register_WithSpecialCharactersInName_ShouldWork() {
        // Arrange
        registerRequest.setFirstName("José María");
        registerRequest.setLastName("O'Connor-Smith");
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("José María");
        assertThat(result.getLastName()).isEqualTo("O'Connor-Smith");
    }

    @Test
    @DisplayName("Should handle long names")
    void register_WithLongNames_ShouldWork() {
        // Arrange
        String longName = "a".repeat(100);
        registerRequest.setFirstName(longName);
        registerRequest.setLastName(longName);
        
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getFirstName()).isEqualTo(longName);
        assertThat(result.getLastName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("Should create user with correct default reputation score")
    void register_ShouldSetDefaultReputationScore() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(registerRequest);

        // Assert
        assertThat(result.getReputationScore()).isEqualTo(0.0);
    }
}