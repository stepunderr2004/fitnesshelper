package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trainer_working_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrainerWorkingDay extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainerId", nullable = false)
    @JsonIgnore
    private User trainer;

    private LocalDate date;

    @ElementCollection
    @CollectionTable(name = "trainer_working_day_hours", joinColumns = @JoinColumn(name = "workingDayId"))
    @Column(name = "hour")
    private Set<Integer> enabledHours = new HashSet<>();
}