package org.uj.project.tidarobot.auth.dto;

public record RegisterRequest(String username,
                              String email,
                              String password,
                              String loginTidaro,
                              String passwordTidaro) {
}
