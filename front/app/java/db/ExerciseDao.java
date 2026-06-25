package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertExercise(Exercise exercise);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertExerciseWithReturn(Exercise exercise);

    @Update
    void updateExercise(Exercise exercise);

    @Delete
    void deleteExercise(Exercise exercise);

    @Query("SELECT * FROM exercises WHERE trainingDayId = :dayId ORDER BY orderIndex ASC")
    List<Exercise> getExercisesForDay(int dayId);
}