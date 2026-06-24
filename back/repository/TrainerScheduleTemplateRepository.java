package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainerScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrainerScheduleTemplateRepository extends JpaRepository<TrainerScheduleTemplate, Integer> {
    List<TrainerScheduleTemplate> findByTrainerId(int trainerId);
    int countByTrainerId(int trainerId);
    Optional<TrainerScheduleTemplate> findByIdAndTrainerId(int id, int trainerId);
}