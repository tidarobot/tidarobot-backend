package org.uj.project.tidarobot.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.exception.UserNotFoundException;
import org.uj.project.tidarobot.user.dto.UpdateUserRequest;
import org.uj.project.tidarobot.user.dto.UserResponse;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;
import org.uj.project.tidarobot.user.repository.UserRepository;
import org.uj.project.tidarobot.user.repository.UserSpecification;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void updateUserStatus(Long userId, Status status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User: " + userId + " not found"));

        user.setStatus(status);

        userRepository.save(user);
    }

    public User updateUser(Long userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User: " + userId + " not found"));

        if(request.email() != null && !request.email().equals(user.getEmail())) {

            if(userRepository.existsByEmail(request.email()))
                throw new RuntimeException("Email already exists");

            user.setEmail(request.email());
        }

        if(request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if(request.loginTidaro() != null) {
            user.setLoginTidaro(request.loginTidaro());
        }

        if(request.passwordTidaro() != null) {
            user.setPasswordTidaro(request.passwordTidaro());
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public Page<UserResponse> getUsers(Pageable pageable, Role role, Status status, String username) {
        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(role))
                .and(UserSpecification.hasStatus(status))
                .and(UserSpecification.usernameContains(username));

        return userRepository.findAll(spec, pageable).map(this::toUserResponse);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
