package org.uj.project.tidarobot.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uj.project.tidarobot.auth.dto.AuthResponse;
import org.uj.project.tidarobot.auth.dto.LoginRequest;
import org.uj.project.tidarobot.auth.dto.RegisterRequest;
import org.uj.project.tidarobot.exception.UserExistsException;
import org.uj.project.tidarobot.security.EncryptionService;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;
import org.uj.project.tidarobot.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EncryptionService encryptionService;
    @InjectMocks AuthService authService;

    @Test
    void register_duplicateUsername_throwsUserExistsException() {
        when(userRepository.existsByUsername("user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user", "user@test.com", "pass", "tidaro@test.com", "tidaroPass")))
                .isInstanceOf(UserExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsUserExistsException() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user", "user@test.com", "pass", "tidaro@test.com", "tidaroPass")))
                .isInstanceOf(UserExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_happyPath_encodesPasswordEncryptsTidaroAndReturnsToken() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashedPass");
        when(encryptionService.encrypt("tidaroPass")).thenReturn("encryptedTidaroPass");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("user", "user@test.com", "pass", "tidaro@test.com", "tidaroPass"));

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("user")
                && u.getEmail().equals("user@test.com")
                && u.getPasswordHash().equals("hashedPass")
                && u.getPasswordTidaro().equals("encryptedTidaroPass")
                && u.getRole() == Role.USER
                && u.getStatus() == Status.PENDING
        ));
    }

    @Test
    void login_happyPath_authenticatesAndReturnsToken() {
        User user = User.builder()
                .username("user")
                .passwordHash("hashedPass")
                .role(Role.USER)
                .status(Status.APPROVED)
                .build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("user", "pass"));

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user", "pass"));
    }
}
