package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_exercise_sets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DiaryExerciseSet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diaryExerciseId", nullable = false)
    @JsonIgnore
    private DiaryExercise diaryExercise;

    private int setNumber;
    private int reps;
    private float weight;
}