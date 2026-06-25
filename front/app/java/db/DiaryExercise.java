package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "diary_exercises",
        foreignKeys = @ForeignKey(entity = DiaryEntry.class,
                parentColumns = "id",
                childColumns = "diaryEntryId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("diaryEntryId"))
public class DiaryExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int diaryEntryId;
    public int orderIndex;
    public String name;
    public String muscleGroup;

    @Ignore
    public List<DiaryExerciseSet> sets = new ArrayList<>();

    @Ignore
    public DiaryExercise() {}

    public DiaryExercise(int diaryEntryId, int orderIndex, String name, String muscleGroup) {
        this.diaryEntryId = diaryEntryId;
        this.orderIndex = orderIndex;
        this.name = name;
        this.muscleGroup = muscleGroup;
    }
}