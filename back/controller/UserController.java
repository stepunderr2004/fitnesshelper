package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable int id, HttpServletRequest request) {
        int currentUserId = (int) request.getAttribute("userId");
        User currentUser = userService.findById(currentUserId);

        // Если запрашивает самого себя – разрешено
        if (currentUserId == id) {
            return userToResponse(userService.findById(id));
        }

        // Если текущий пользователь – клиент, и запрашивает своего тренера
        if (currentUser != null && "CLIENT".equals(currentUser.getRole())
                && currentUser.getTrainer() != null && currentUser.getTrainer().getId() == id) {
            User trainer = userService.findById(id);
            if (trainer != null && "TRAINER".equals(trainer.getRole())) {
                return userToResponse(trainer);
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable int id,
                                           @RequestBody User updatedUser,
                                           HttpServletRequest request) {
        int currentUserId = (int) request.getAttribute("userId");
        if (currentUserId != id) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        User user = userService.updateProfile(id, updatedUser.getName(), updatedUser.getAge(),
                updatedUser.getGender(), updatedUser.isTrainerEnabled());
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable int id,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        int currentUserId = (int) request.getAttribute("userId");
        if (currentUserId != id) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        String token = body.get("fcmToken");
        userService.updateFcmToken(id, token);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Map<String, Object>> userToResponse(User user) {
        if (user == null) return ResponseEntity.notFound().build();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("age", user.getAge());
        map.put("gender", user.getGender());
        map.put("trainerEnabled", user.isTrainerEnabled());
        map.put("role", user.getRole());
        map.put("trainerId", user.getTrainer() != null ? user.getTrainer().getId() : null);
        return ResponseEntity.ok(map);
    }
}