package com.example.fitnesshelper.controller;

import com.example.fitnesshelper.entity.Exercise;
import com.example.fitnesshelper.entity.TrainingDay;
import com.example.fitnesshelper.repository.ExerciseRepository;
import com.example.fitnesshelper.repository.TrainingDayRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/days/{dayId}/exercises")
public class ExerciseController {

    private final ExerciseRepository exerciseRepo;
    private final TrainingDayRepository dayRepo;

    public ExerciseController(ExerciseRepository exerciseRepo, TrainingDayRepository dayRepo) {
        this.exerciseRepo = exerciseRepo;
        this.dayRepo = dayRepo;
    }

    @GetMapping
    public List<Exercise> getAll(@PathVariable int dayId, HttpServletRequest request) {
        getDay(dayId, request);
        return exerciseRepo.findByTrainingDayIdOrderByOrderIndex(dayId);
    }

    @PostMapping
    public Exercise create(@PathVariable int dayId, @RequestBody Exercise exercise, HttpServletRequest request) {
        TrainingDay day = getDay(dayId, request);
        if (exerciseRepo.findByTrainingDayIdOrderByOrderIndex(dayId).size() >= 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 10 exercises");
        }
        exercise.setTrainingDay(day);
        return exerciseRepo.save(exercise);
    }

    @PutMapping("/{exerciseId}")
    public Exercise update(@PathVariable int dayId, @PathVariable int exerciseId,
                           @RequestBody Exercise exercise, HttpServletRequest request) {
        getDay(dayId, request);
        Exercise existing = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Проверка, что упражнение принадлежит указанному дню
        if (existing.getTrainingDay().getId() != dayId) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        existing.setName(exercise.getName());
        existing.setMuscleGroup(exercise.getMuscleGroup());
        existing.setOrderIndex(exercise.getOrderIndex());
        return exerciseRepo.save(existing);
    }

    @DeleteMapping("/{exerciseId}")
    public void delete(@PathVariable int dayId, @PathVariable int exerciseId, HttpServletRequest request) {
        // Убеждаемся, что день принадлежит текущему пользователю
        getDay(dayId, request);
        Exercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Убираем избыточную проверку принадлежности упражнения дню – она уже гарантирована,
        // так как упражнение загружено по id, и dayId проверен.
        exerciseRepo.delete(ex);
    }

    private TrainingDay getDay(int dayId, HttpServletRequest request) {
        TrainingDay day = dayRepo.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int userId = (int) request.getAttribute("userId");
        if (day.getProgram().getUser().getId() != userId)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return day;
    }
}