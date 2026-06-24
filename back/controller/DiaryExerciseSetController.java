package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.DiaryExercise;
import com.example.fitnesshelper.entity.DiaryExerciseSet;
import com.example.fitnesshelper.repository.DiaryExerciseRepository;
import com.example.fitnesshelper.repository.DiaryExerciseSetRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/diary-exercises/{exerciseId}/sets")
public class DiaryExerciseSetController {

    private final DiaryExerciseSetRepository setRepo;
    private final DiaryExerciseRepository exerciseRepo;

    public DiaryExerciseSetController(DiaryExerciseSetRepository setRepo, DiaryExerciseRepository exerciseRepo) {
        this.setRepo = setRepo;
        this.exerciseRepo = exerciseRepo;
    }

    @GetMapping
    public List<DiaryExerciseSet> getAll(@PathVariable int exerciseId, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        return setRepo.findByDiaryExerciseIdOrderBySetNumber(exerciseId);
    }

    @PostMapping
    public DiaryExerciseSet create(@PathVariable int exerciseId, @RequestBody DiaryExerciseSet set, HttpServletRequest request) {
        DiaryExercise ex = checkAccess(exerciseId, request);
        set.setDiaryExercise(ex);
        return setRepo.save(set);
    }

    @PutMapping("/{setId}")
    public DiaryExerciseSet update(@PathVariable int exerciseId, @PathVariable int setId,
                                   @RequestBody DiaryExerciseSet updated, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        DiaryExerciseSet set = setRepo.findById(setId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getDiaryExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        set.setReps(updated.getReps());
        set.setWeight(updated.getWeight());
        set.setSetNumber(updated.getSetNumber());
        return setRepo.save(set);
    }

    @DeleteMapping("/{setId}")
    public void delete(@PathVariable int exerciseId, @PathVariable int setId, HttpServletRequest request) {
        checkAccess(exerciseId, request);
        DiaryExerciseSet set = setRepo.findById(setId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (set.getDiaryExercise().getId() != exerciseId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        setRepo.delete(set);
    }

    private DiaryExercise checkAccess(int exerciseId, HttpServletRequest request) {
        DiaryExercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int userId = (int) request.getAttribute("userId");
        if (ex.getDiaryEntry().getUser().getId() != userId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ex;
    }
}