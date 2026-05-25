package org.uj.project.tidarobot.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uj.project.tidarobot.exception.UserNotFoundException;
import org.uj.project.tidarobot.security.EncryptionService;
import org.uj.project.tidarobot.user.dto.UpdateUserRequest;
import org.uj.project.tidarobot.user.dto.UserResponse;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;
import org.uj.project.tidarobot.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EncryptionService encryptionService;
    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("old@test.com")
                .passwordHash("oldHash")
                .loginTidaro("old-tidaro@test.com")
                .passwordTidaro("encryptedOldPass")
                .role(Role.USER)
                .status(Status.PENDING)
                .build();
    }

    // --- updateUserStatus ---

    @Test
    void updateUserStatus_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(99L, Status.APPROVED))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserStatus_happyPath_updatesStatusAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserStatus(1L, Status.APPROVED);

        assertThat(user.getStatus()).isEqualTo(Status.APPROVED);
        verify(userRepository).save(user);
    }

    // --- updateUser ---

    @Test
    void updateUser_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest("new@test.com", null, null, null)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUser_duplicateEmail_throwsRuntimeException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest("taken@test.com", null, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateUser_onlyEmailChanged_updatesEmailLeavesPasswordUntouched() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, new UpdateUserRequest("new@test.com", null, null, null));

        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getPasswordHash()).isEqualTo("oldHash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_sameEmailAsCurrentUser_skipsEmailUpdate() {
        // email unchanged — the if-branch checks !request.email().equals(user.getEmail())
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, new UpdateUserRequest("old@test.com", null, null, null));

        verify(userRepository, never()).existsByEmail(any());
        assertThat(user.getEmail()).isEqualTo("old@test.com");
    }

    @Test
    void updateUser_onlyPasswordChanged_encodesPasswordLeavesEmailUntouched() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, new UpdateUserRequest(null, "newPass", null, null));

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.getEmail()).isEqualTo("old@test.com");
    }

    @Test
    void updateUser_onlyTidaroFieldsChanged_encryptsPasswordAndUpdatesLogin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(encryptionService.encrypt("newTidaroPass")).thenReturn("encryptedNew");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, new UpdateUserRequest(null, null, "new-tidaro@test.com", "newTidaroPass"));

        assertThat(user.getLoginTidaro()).isEqualTo("new-tidaro@test.com");
        assertThat(user.getPasswordTidaro()).isEqualTo("encryptedNew");
        verify(passwordEncoder, never()).encode(any());
    }

    // --- deleteUser ---

    @Test
    void deleteUser_delegatesToRepositoryDeleteById() {
        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    // --- getUsers ---

    @Test
    void getUsers_allFiltersNull_delegatesToRepositoryWithSpec() {
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<UserResponse> result = userService.getUsers(PageRequest.of(0, 10), null, null, null);

        assertThat(result).isEmpty();
        verify(userRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getUsers_withFilters_mapsUsersToUserResponse() {
        LocalDateTime createdAt = LocalDateTime.now();
        User found = User.builder()
                .id(5L)
                .username("alice")
                .email("alice@test.com")
                .role(Role.USER)
                .status(Status.APPROVED)
                .createdAt(createdAt)
                .build();

        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(found)));

        Page<UserResponse> result = userService.getUsers(PageRequest.of(0, 10), Role.USER, Status.APPROVED, "alice");

        assertThat(result).hasSize(1);
        UserResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.status()).isEqualTo(Status.APPROVED);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
