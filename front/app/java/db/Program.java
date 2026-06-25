package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "programs",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("userId"))
public class Program {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String name;

    @Ignore
    public List<TrainingDay> days = new ArrayList<>();

    public Program(int userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}