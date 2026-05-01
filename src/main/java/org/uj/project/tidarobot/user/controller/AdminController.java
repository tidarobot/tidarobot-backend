package org.uj.project.tidarobot.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uj.project.tidarobot.user.dto.UserResponse;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.service.UserService;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            Pageable pageable,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String username
    ) {
        return ResponseEntity.ok(userService.getUsers(pageable, role, status, username));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Status status
    ) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
