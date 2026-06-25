package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface TrainingDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TrainingDay day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWithReturn(TrainingDay day);

    @Delete
    void delete(TrainingDay day);

    @Query("SELECT * FROM training_days WHERE programId = :programId ORDER BY id ASC")
    List<TrainingDay> getDaysForProgram(int programId);

    @Query("SELECT COUNT(*) FROM training_days WHERE programId = :programId")
    int getDayCountForProgram(int programId);

    @Query("DELETE FROM training_days WHERE programId = :programId")
    void deleteAllForProgram(int programId);
}