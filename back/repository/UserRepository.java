package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByRefreshToken(String refreshToken);
    List<User> findByTrainerId(int trainerId);
}