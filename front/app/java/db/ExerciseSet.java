package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "exercise_sets",
        foreignKeys = @ForeignKey(entity = Exercise.class,
                parentColumns = "id",
                childColumns = "exerciseId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("exerciseId"))
public class ExerciseSet {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int exerciseId;
    public int setNumber;   // 1, 2, 3...
    public int reps;
    public float weight;

    @Ignore
    public ExerciseSet() {}

    public ExerciseSet(int exerciseId, int setNumber, int reps, float weight) {
        this.exerciseId = exerciseId;
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }
}