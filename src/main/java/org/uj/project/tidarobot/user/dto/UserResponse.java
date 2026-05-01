package org.uj.project.tidarobot.user.dto;

import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        Status status,
        LocalDateTime createdAt
) {}
