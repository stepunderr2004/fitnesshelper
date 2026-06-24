package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trainer_schedule_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrainerScheduleTemplate extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainerId", nullable = false)
    @JsonIgnore
    private User trainer;

    @Column(nullable = false)
    private String name;

    private boolean allHoursEnabled;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TrainerScheduleDay> days = new HashSet<>();
}