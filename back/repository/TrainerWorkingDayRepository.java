package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainerWorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainerWorkingDayRepository extends JpaRepository<TrainerWorkingDay, Integer> {
    List<TrainerWorkingDay> findByTrainerIdAndDateBetween(int trainerId, LocalDate from, LocalDate to);
    Optional<TrainerWorkingDay> findByTrainerIdAndDate(int trainerId, LocalDate date);

    @Modifying
    @Transactional
    @Query("DELETE FROM TrainerWorkingDay wd WHERE wd.date < :threshold")
    void deleteByDateBefore(LocalDate threshold);
}