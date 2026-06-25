package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface TrainingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSession(TrainingSession session);

    @Query("SELECT * FROM training_sessions WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    TrainingSession getLastSessionForUser(int userId);

    @Query("DELETE FROM training_sessions WHERE userId = :userId")
    void deleteAllForUser(int userId);
}