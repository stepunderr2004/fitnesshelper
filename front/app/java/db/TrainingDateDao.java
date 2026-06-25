package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TrainingDateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDate(TrainingDate trainingDate);

    @Query("SELECT * FROM training_dates WHERE userId = :userId")
    List<TrainingDate> getDatesForUser(int userId);

    @Query("DELETE FROM training_dates WHERE userId = :userId AND date = :date")
    void deleteDateByUserAndDate(int userId, String date);

    @Query("DELETE FROM training_dates WHERE userId = :userId")
    void deleteAllForUser(int userId);
}