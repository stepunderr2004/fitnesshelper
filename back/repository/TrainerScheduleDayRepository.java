package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainerScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TrainerScheduleDayRepository extends JpaRepository<TrainerScheduleDay, Integer> {
    Optional<TrainerScheduleDay> findByIdAndTemplate_TrainerId(int dayId, int trainerId);
}