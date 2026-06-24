package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trainer_schedule_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrainerScheduleDay extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateId", nullable = false)
    @JsonIgnore
    private TrainerScheduleTemplate template;

    private int dayOfWeek; // 1=ПН, 2=ВТ, ..., 7=ВС

    @ElementCollection
    @CollectionTable(name = "trainer_schedule_day_hours", joinColumns = @JoinColumn(name = "dayId"))
    @Column(name = "hour")
    private Set<Integer> enabledHours = new HashSet<>();
}