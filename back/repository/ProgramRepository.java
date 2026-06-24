package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Integer> {
    List<Program> findByUserId(int userId);
    int countByUserId(int userId);
    void deleteAllByUserId(int userId);
}