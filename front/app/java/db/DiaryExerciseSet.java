package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "diary_exercise_sets",
        foreignKeys = @ForeignKey(entity = DiaryExercise.class,
                parentColumns = "id",
                childColumns = "diaryExerciseId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("diaryExerciseId"))
public class DiaryExerciseSet {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int diaryExerciseId;
    public int setNumber;
    public int reps;
    public float weight;

    @Ignore
    public DiaryExerciseSet() {}

    public DiaryExerciseSet(int diaryExerciseId, int setNumber, int reps, float weight) {
        this.diaryExerciseId = diaryExerciseId;
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }
}