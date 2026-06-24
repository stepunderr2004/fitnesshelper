package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Integer> {
    Optional<TrainingSession> findTopByUserIdOrderByIdDesc(int userId);
    void deleteAllByUserId(int userId);
}