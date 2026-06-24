package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.TrainingSession;
import com.example.fitnesshelper.repository.TrainingSessionRepository;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {

    private final TrainingSessionRepository repo;
    private final UserService userService;

    public TrainingSessionController(TrainingSessionRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @GetMapping("/last")
    public TrainingSession getLast(HttpServletRequest request) {
        return repo.findTopByUserIdOrderByIdDesc(getUserId(request)).orElse(null);
    }

    @PostMapping
    public TrainingSession create(@RequestBody TrainingSession session, HttpServletRequest request) {
        session.setUser(userService.findById(getUserId(request)));
        return repo.save(session);
    }

    private int getUserId(HttpServletRequest request) {
        return (int) request.getAttribute("userId");
    }
}