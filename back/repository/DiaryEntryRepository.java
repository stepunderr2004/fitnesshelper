package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.DiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Integer> {
    List<DiaryEntry> findByUserIdOrderByDateDesc(int userId);
    List<DiaryEntry> findByUserId(int userId);
    DiaryEntry findByUserIdAndDate(int userId, String date);
    void deleteAllByUserId(int userId);
}