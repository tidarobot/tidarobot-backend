package org.uj.project.tidarobot.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uj.project.tidarobot.user.service.UserService;
import org.uj.project.tidarobot.user.entity.Status;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
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
