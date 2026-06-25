package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DiaryExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSet(DiaryExerciseSet set);

    @Update
    void updateSet(DiaryExerciseSet set);

    @Delete
    void deleteSet(DiaryExerciseSet set);

    @Query("SELECT * FROM diary_exercise_sets WHERE diaryExerciseId = :exerciseId ORDER BY setNumber ASC")
    List<DiaryExerciseSet> getSetsForExercise(int exerciseId);
}