package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingDayRepository extends JpaRepository<TrainingDay, Integer> {
    List<TrainingDay> findByProgramId(int programId);
    int countByProgramId(int programId);
    void deleteAllByProgramId(int programId);
}