package org.uj.project.tidarobot.user.dto;

public record UpdateUserRequest(
        String email,
        String password,
        String loginTidaro,
        String passwordTidaro
) {}
