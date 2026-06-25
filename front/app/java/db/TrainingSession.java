package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "training_sessions",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("userId"))
public class TrainingSession {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int totalSeconds;
    public float tonnage;

    @Ignore
    public TrainingSession() {}

    public TrainingSession(int userId, int totalSeconds, float tonnage) {
        this.userId = userId;
        this.totalSeconds = totalSeconds;
        this.tonnage = tonnage;
    }
}