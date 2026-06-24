package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.DiaryExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiaryExerciseRepository extends JpaRepository<DiaryExercise, Integer> {
    List<DiaryExercise> findByDiaryEntryIdOrderByOrderIndex(int diaryEntryId);
    List<DiaryExercise> findByDiaryEntryId(int diaryEntryId);
    void deleteAllByDiaryEntryId(int diaryEntryId);
}