package com.example.fitnesshelper.service;

import com.example.fitnesshelper.repository.TrainerWorkingDayRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ScheduleCleanupService {

    private final TrainerWorkingDayRepository workingDayRepo;

    public ScheduleCleanupService(TrainerWorkingDayRepository workingDayRepo) {
        this.workingDayRepo = workingDayRepo;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupOldWorkingDays() {
        LocalDate threshold = LocalDate.now().minusDays(7);
        workingDayRepo.deleteByDateBefore(threshold);
    }
}