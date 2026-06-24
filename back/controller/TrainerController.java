package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.service.FcmService;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    private final UserService userService;
    private final FcmService fcmService;

    public TrainerController(UserService userService, FcmService fcmService) {
        this.userService = userService;
        this.fcmService = fcmService;
    }

    @GetMapping("/clients")
    public Map<String, List<Map<String, Object>>> getClients(HttpServletRequest request) {
        int trainerId = (int) request.getAttribute("userId");
        User trainer = userService.findById(trainerId);
        if (trainer == null || !"TRAINER".equals(trainer.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<User> myClients = userService.getAllUsers().stream()
                .filter(u -> u.getTrainer() != null && u.getTrainer().getId() == trainerId)
                .collect(Collectors.toList());
        List<User> independentClients = userService.getAllUsers().stream()
                .filter(u -> "CLIENT".equals(u.getRole()) && u.getTrainer() == null)
                .collect(Collectors.toList());

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("myClients", myClients.stream().map(this::convertToMap).collect(Collectors.toList()));
        result.put("independentClients", independentClients.stream().map(this::convertToMap).collect(Collectors.toList()));
        return result;
    }

    @PutMapping("/clients/{clientId}/assign")
    public ResponseEntity<Void> assignClient(@PathVariable int clientId, HttpServletRequest request) {
        int trainerId = (int) request.getAttribute("userId");
        User trainer = userService.findById(trainerId);
        if (trainer == null || !"TRAINER".equals(trainer.getRole())) {
            return ResponseEntity.status(403).build();
        }
        User client = userService.findById(clientId);
        // Исправлено условие: trainerId может быть 0 у старых клиентов
        if (client == null || !"CLIENT".equals(client.getRole()) ||
                (client.getTrainer() != null && client.getTrainer().getId() != 0)) {
            return ResponseEntity.badRequest().build();
        }
        client.setTrainer(trainer);
        client.setLastModified(System.currentTimeMillis());
        userService.saveUser(client);

        if (client.getFcmToken() != null && !client.getFcmToken().isEmpty()) {
            fcmService.sendSyncNotification(client.getFcmToken());
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clients/{clientId}/unassign")
    public ResponseEntity<Void> unassignClient(@PathVariable int clientId, HttpServletRequest request) {
        int trainerId = (int) request.getAttribute("userId");
        User trainer = userService.findById(trainerId);
        if (trainer == null || !"TRAINER".equals(trainer.getRole())) {
            return ResponseEntity.status(403).build();
        }
        User client = userService.findById(clientId);
        // Исправлено условие: trainerId == 0 считаем отсутствием тренера
        if (client == null || client.getTrainer() == null || client.getTrainer().getId() == 0 ||
                client.getTrainer().getId() != trainerId) {
            return ResponseEntity.badRequest().build();
        }
        client.setTrainer(null);
        client.setLastModified(System.currentTimeMillis());
        userService.saveUser(client);

        if (client.getFcmToken() != null && !client.getFcmToken().isEmpty()) {
            fcmService.sendSyncNotification(client.getFcmToken());
        }
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> convertToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("trainerId", user.getTrainer() != null ? user.getTrainer().getId() : null);
        return map;
    }
}