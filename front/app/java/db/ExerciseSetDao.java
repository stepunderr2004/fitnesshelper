package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSet(ExerciseSet set);

    @Update
    void updateSet(ExerciseSet set);

    @Delete
    void deleteSet(ExerciseSet set);

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC")
    List<ExerciseSet> getSetsForExercise(int exerciseId);
}