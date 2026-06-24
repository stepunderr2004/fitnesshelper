package com.example.fitnesshelper.repository;

import com.example.fitnesshelper.entity.ClientBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ClientBookingRepository extends JpaRepository<ClientBooking, Integer> {
    List<ClientBooking> findByClientIdAndActiveTrue(int clientId);
    List<ClientBooking> findByTrainerIdAndBookingDateAndActiveTrue(int trainerId, LocalDate date);
    boolean existsByTrainerIdAndBookingDateAndActiveTrue(int trainerId, LocalDate date);
    boolean existsByClientIdAndBookingDateAndHour(int clientId, LocalDate date, int hour);
    int countByClientIdAndActiveTrue(int clientId);
}