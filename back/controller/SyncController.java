package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.service.UserService;
import com.example.fitnesshelper.service.LastModifiedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SyncController {

    private final LastModifiedService lastModifiedService;
    private final UserService userService;

    public SyncController(LastModifiedService lastModifiedService, UserService userService) {
        this.lastModifiedService = lastModifiedService;
        this.userService = userService;
    }

    @GetMapping("/users/{userId}/last-modified")
    public ResponseEntity<Long> getLastModified(@PathVariable int userId, HttpServletRequest request) {
        int currentUserId = (int) request.getAttribute("userId");
        // Только сам пользователь (или будущий тренер) может запрашивать lastModified
        if (currentUserId != userId) {
            return ResponseEntity.status(403).build();
        }
        User user = userService.findById(userId);
        if (user == null) return ResponseEntity.notFound().build();
        long max = lastModifiedService.getMaxLastModified(userId);
        return ResponseEntity.ok(max);
    }
}