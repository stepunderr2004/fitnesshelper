package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "diary_entries",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("userId"))
public class DiaryEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String date;

    @Ignore
    public List<DiaryExercise> exercises = new ArrayList<>();

    @Ignore
    public DiaryEntry() {}

    public DiaryEntry(int userId, String date) {
        this.userId = userId;
        this.date = date;
    }
}