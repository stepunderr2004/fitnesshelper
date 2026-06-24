package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainingDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingDateRepository extends JpaRepository<TrainingDate, Integer> {
    List<TrainingDate> findByUserId(int userId);
    void deleteByUserIdAndDate(int userId, String date);
    void deleteAllByUserId(int userId);
}