package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.config.JwtUtil;
import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.service.LastModifiedService;
import com.example.fitnesshelper.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final LastModifiedService lastModifiedService;

    public AuthController(UserService userService, JwtUtil jwtUtil, LastModifiedService lastModifiedService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.lastModifiedService = lastModifiedService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        User user = userService.login(username, password);
        if (user != null) {
            String accessToken = jwtUtil.generateToken(user.getId(), user.getRole());
            String refreshToken = UUID.randomUUID().toString();
            userService.updateRefreshToken(user.getId(), refreshToken);
            long lastModified = lastModifiedService.getMaxLastModified(user.getId());
            return ResponseEntity.ok(Map.of(
                    "token", accessToken,
                    "refreshToken", refreshToken,
                    "userId", user.getId(),
                    "role", user.getRole(),
                    "lastModified", lastModified
            ));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        User user = userService.findByRefreshToken(refreshToken);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
        // Генерируем новую пару токенов
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getRole());
        String newRefreshToken = UUID.randomUUID().toString();
        userService.updateRefreshToken(user.getId(), newRefreshToken);
        return ResponseEntity.ok(Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }
}