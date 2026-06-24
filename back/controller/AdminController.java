package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return users.stream()
                .filter(u -> !"ADMIN".equals(u.getRole()))
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> payload) {
        User user = userService.createUser(payload);
        return ResponseEntity.ok(convertToMap(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        User user = userService.updateUserByAdmin(id, payload);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(convertToMap(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable int id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при удалении: " + e.getMessage());
        }
    }

    private Map<String, Object> convertToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("age", user.getAge());
        map.put("gender", user.getGender());
        map.put("role", user.getRole());
        map.put("trainerEnabled", user.isTrainerEnabled());

        Integer trainerId = null;
        try {
            if (user.getTrainer() != null) {
                trainerId = user.getTrainer().getId();
            }
        } catch (Exception ignored) {
        }
        map.put("trainerId", trainerId);

        return map;
    }
}