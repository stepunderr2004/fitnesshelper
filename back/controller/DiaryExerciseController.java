package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.DiaryEntry;
import com.example.fitnesshelper.entity.DiaryExercise;
import com.example.fitnesshelper.repository.DiaryEntryRepository;
import com.example.fitnesshelper.repository.DiaryExerciseRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/diary-entries/{entryId}/exercises")
public class DiaryExerciseController {

    private final DiaryExerciseRepository exerciseRepo;
    private final DiaryEntryRepository entryRepo;

    public DiaryExerciseController(DiaryExerciseRepository exerciseRepo, DiaryEntryRepository entryRepo) {
        this.exerciseRepo = exerciseRepo;
        this.entryRepo = entryRepo;
    }

    @GetMapping
    public List<DiaryExercise> getAll(@PathVariable int entryId, HttpServletRequest request) {
        getEntry(entryId, request);
        return exerciseRepo.findByDiaryEntryIdOrderByOrderIndex(entryId);
    }

    @PostMapping
    public DiaryExercise create(@PathVariable int entryId, @RequestBody DiaryExercise exercise, HttpServletRequest request) {
        DiaryEntry entry = getEntry(entryId, request);
        exercise.setDiaryEntry(entry);
        return exerciseRepo.save(exercise);
    }

    @PutMapping("/{exerciseId}")
    public DiaryExercise update(@PathVariable int entryId, @PathVariable int exerciseId,
                                @RequestBody DiaryExercise updated, HttpServletRequest request) {
        getEntry(entryId, request);
        DiaryExercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getId() != entryId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ex.setName(updated.getName());
        ex.setMuscleGroup(updated.getMuscleGroup());
        ex.setOrderIndex(updated.getOrderIndex());
        return exerciseRepo.save(ex);
    }

    @DeleteMapping("/{exerciseId}")
    public void delete(@PathVariable int entryId, @PathVariable int exerciseId, HttpServletRequest request) {
        getEntry(entryId, request);
        DiaryExercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ex.getDiaryEntry().getId() != entryId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // каскадное удаление подходов сработает автоматически
        exerciseRepo.delete(ex);
    }

    private DiaryEntry getEntry(int entryId, HttpServletRequest request) {
        DiaryEntry entry = entryRepo.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int userId = (int) request.getAttribute("userId");
        if (entry.getUser().getId() != userId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return entry;
    }
}