package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.Exercise;
import com.example.fitnesshelper.entity.ExerciseSet;
import com.example.fitnesshelper.repository.ExerciseRepository;
import com.example.fitnesshelper.repository.ExerciseSetRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/exercises/{exerciseId}/sets")
public class ExerciseSetController {

    private final ExerciseSetRepository setRepo;
    private final ExerciseRepository exerciseRepo;

    public ExerciseSetController(ExerciseSetRepository setRepo, ExerciseRepository exerciseRepo) {
        this.setRepo = setRepo;
        this.exerciseRepo = exerciseRepo;
    }

    @GetMapping
    public List<ExerciseSet> getAll(@PathVariable int exerciseId, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        return setRepo.findByExerciseIdOrderBySetNumber(exerciseId);
    }

    @PostMapping
    public ExerciseSet create(@PathVariable int exerciseId, @RequestBody ExerciseSet set, HttpServletRequest request) {
        Exercise ex = checkAccess(exerciseId, request);
        set.setExercise(ex);
        return setRepo.save(set);
    }

    @PutMapping("/{setId}")
    public ExerciseSet update(@PathVariable int exerciseId, @PathVariable int setId,
                              @RequestBody ExerciseSet updated, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        ExerciseSet set = setRepo.findById(setId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setReps(updated.getReps());
        set.setWeight(updated.getWeight());
        set.setSetNumber(updated.getSetNumber());
        return setRepo.save(set);
    }

    @DeleteMapping("/{setId}")
    public void delete(@PathVariable int exerciseId, @PathVariable int setId, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        ExerciseSet set = setRepo.findById(setId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        setRepo.delete(set);
    }

    private Exercise checkAccess(int exerciseId, HttpServletRequest request) {
        Exercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int userId = (int) request.getAttribute("userId");
        if (ex.getTrainingDay().getProgram().getUser().getId() != userId)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ex;
    }
}