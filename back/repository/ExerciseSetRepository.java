package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.ExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Integer> {
    List<ExerciseSet> findByExerciseIdOrderBySetNumber(int exerciseId);
    void deleteAllByExerciseId(int exerciseId);
}