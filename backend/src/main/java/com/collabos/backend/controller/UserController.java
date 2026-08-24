package com.collabos.backend.controller;

import com.collabos.backend.dto.UserResponse;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new UserResponse(currentUser.id(), currentUser.name(), currentUser.email());
    }
}
