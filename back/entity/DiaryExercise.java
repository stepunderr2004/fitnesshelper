package com.example.fitnesshelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diary_exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DiaryExercise extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diaryEntryId", nullable = false)
    @JsonIgnore
    private DiaryEntry diaryEntry;

    private int orderIndex;
    private String name;
    private String muscleGroup;

    @OneToMany(mappedBy = "diaryExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private List<DiaryExerciseSet> sets = new ArrayList<>();
}