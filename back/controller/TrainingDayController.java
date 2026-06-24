package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.Program;
import com.example.fitnesshelper.entity.TrainingDay;
import com.example.fitnesshelper.entity.User;
import com.example.fitnesshelper.repository.ProgramRepository;
import com.example.fitnesshelper.repository.TrainingDayRepository;
import com.example.fitnesshelper.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/programs/{programId}/days")
public class TrainingDayController {

    private final TrainingDayRepository dayRepo;
    private final ProgramRepository programRepo;
    private final UserService userService;

    public TrainingDayController(TrainingDayRepository dayRepo, ProgramRepository programRepo, UserService userService) {
        this.dayRepo = dayRepo;
        this.programRepo = programRepo;
        this.userService = userService;
    }

    @GetMapping
    public List<TrainingDay> getAll(@PathVariable int programId, HttpServletRequest request) {
        checkAccess(programId, request);
        return dayRepo.findByProgramId(programId);
    }

    @PostMapping
    public TrainingDay create(@PathVariable int programId, @RequestBody TrainingDay day, HttpServletRequest request) {
        Program program = getProgram(programId, request);
        if (dayRepo.countByProgramId(programId) >= 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 8 days");
        }
        day.setProgram(program);
        return dayRepo.save(day);
    }

    @DeleteMapping("/{dayId}")
    public void delete(@PathVariable int programId, @PathVariable int dayId, HttpServletRequest request) {
        checkAccess(programId, request);
        TrainingDay day = dayRepo.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (day.getProgram().getId() != programId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        dayRepo.delete(day);
    }

    private Program getProgram(int programId, HttpServletRequest request) {
        Program p = programRepo.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getUser().getId() != getCurrentUser(request).getId())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return p;
    }

    private void checkAccess(int programId, HttpServletRequest request) {
        getProgram(programId, request);
    }

    private User getCurrentUser(HttpServletRequest request) {
        return userService.findById((int) request.getAttribute("userId"));
    }
}