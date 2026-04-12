package org.uj.project.tidarobot.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.uj.project.tidarobot.user.dto.UpdateUserRequest;
import org.uj.project.tidarobot.user.service.UserService;
import org.uj.project.tidarobot.user.entity.User;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/update")
    public ResponseEntity<User> updateUser(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateUserRequest request
    ) {
        User uptatedUser = userService.updateUser(user.getId(), request);
        return ResponseEntity.ok(uptatedUser);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal User user) {
        userService.deleteUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}