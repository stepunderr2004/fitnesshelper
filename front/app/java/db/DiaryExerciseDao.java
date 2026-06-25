package com.example.fitnesshelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface DiaryExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DiaryExercise exercise);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWithReturn(DiaryExercise exercise);

    @Update
    void update(DiaryExercise exercise);

    @Delete
    void delete(DiaryExercise exercise);

    @Query("SELECT * FROM diary_exercises WHERE diaryEntryId = :entryId ORDER BY orderIndex ASC")
    List<DiaryExercise> getExercisesForEntry(int entryId);

    @Query("DELETE FROM diary_exercises WHERE diaryEntryId = :entryId")
    void deleteAllByEntryId(int entryId);
}