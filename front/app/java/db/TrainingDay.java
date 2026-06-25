package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "training_days",
        foreignKeys = @ForeignKey(entity = Program.class,
                parentColumns = "id",
                childColumns = "programId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("programId"))
public class TrainingDay {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int programId;
    public String name;

    @Ignore
    public List<Exercise> exercises = new ArrayList<>();

    @Ignore
    public TrainingDay() {}

    public TrainingDay(int programId, String name) {
        this.programId = programId;
        this.name = name;
    }
}