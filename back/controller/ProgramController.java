package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.Program;
import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.repository.ProgramRepository;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramRepository programRepository;
    private final UserService userService;

    public ProgramController(ProgramRepository programRepository, UserService userService) {
        this.programRepository = programRepository;
        this.userService = userService;
    }

    private User getCurrentUser(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        return userService.findById(userId);
    }

    @GetMapping
    public List<Program> getAll(HttpServletRequest request) {
        return programRepository.findByUserId(getCurrentUser(request).getId());
    }

    @PostMapping
    public Program create(@RequestBody Program program, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (programRepository.countByUserId(user.getId()) >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 programs");
        }
        program.setUser(user);
        return programRepository.save(program);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable int id, HttpServletRequest request) {
        Program p = programRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != getCurrentUser(request).getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // Каскадное удаление дней, упражнений и подходов сработает автоматически
        programRepository.delete(p);
    }
}