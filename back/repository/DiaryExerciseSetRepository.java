package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.DiaryExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiaryExerciseSetRepository extends JpaRepository<DiaryExerciseSet, Integer> {
    List<DiaryExerciseSet> findByDiaryExerciseIdOrderBySetNumber(int diaryExerciseId);
    void deleteAllByDiaryExerciseId(int diaryExerciseId);
}