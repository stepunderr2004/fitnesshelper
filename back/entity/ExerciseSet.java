package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_sets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExerciseSet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exerciseId", nullable = false)
    @JsonIgnore
    private Exercise exercise;

    private int setNumber;
    private int reps;
    private float weight;
}