package com.example.fitnesshelper.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "training_dates",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("userId"))
public class TrainingDate {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String date;

    // Пустой конструктор для Retrofit/Gson (Room игнорирует)
    @Ignore
    public TrainingDate() {}

    public TrainingDate(int userId, String date) {
        this.userId = userId;
        this.date = date;
    }
}