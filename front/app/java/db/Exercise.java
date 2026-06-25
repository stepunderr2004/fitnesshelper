package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "exercises",
        foreignKeys = @ForeignKey(entity = TrainingDay.class,
                parentColumns = "id",
                childColumns = "trainingDayId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("trainingDayId"))
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int trainingDayId;
    public int orderIndex;
    public String name;
    public String muscleGroup;

    @Ignore
    public List<ExerciseSet> sets = new ArrayList<>();

    public Exercise(int trainingDayId, int orderIndex, String name, String muscleGroup) {
        this.trainingDayId = trainingDayId;
        this.orderIndex = orderIndex;
        this.name = name;
        this.muscleGroup = muscleGroup;
    }
}