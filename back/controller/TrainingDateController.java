package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.TrainingDate;
import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.repository.TrainingDateRepository;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/training-dates")
public class TrainingDateController {

    private final TrainingDateRepository repo;
    private final UserService userService;

    public TrainingDateController(TrainingDateRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @GetMapping
    public List<TrainingDate> getAll(HttpServletRequest request) {
        return repo.findByUserId(getUserId(request));
    }

    @PostMapping
    public TrainingDate create(@RequestBody TrainingDate date, HttpServletRequest request) {
        User user = userService.findById(getUserId(request));
        date.setUser(user);
        return repo.save(date);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id, HttpServletRequest request) {
        TrainingDate td = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (td.getUser().getId() != getUserId(request)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        repo.delete(td);
    }

    private int getUserId(HttpServletRequest request) {
        return (int) request.getAttribute("userId");
    }
}