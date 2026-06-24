package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {
    List<Exercise> findByTrainingDayIdOrderByOrderIndex(int trainingDayId);
    List<Exercise> findByTrainingDayId(int trainingDayId);
    void deleteAllByTrainingDayId(int trainingDayId);
}