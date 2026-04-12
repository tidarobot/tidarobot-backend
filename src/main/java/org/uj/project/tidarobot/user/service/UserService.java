package org.uj.project.tidarobot.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uj.project.tidarobot.exception.UserNotFoundException;
import org.uj.project.tidarobot.user.dto.UpdateUserRequest;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;
import org.uj.project.tidarobot.user.repository.UserRepository;

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

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
