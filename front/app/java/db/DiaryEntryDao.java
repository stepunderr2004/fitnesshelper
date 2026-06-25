package com.example.fitnesshelper.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface DiaryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DiaryEntry entry);

    @Update
    void update(DiaryEntry entry);

    @Delete
    void delete(DiaryEntry entry);

    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    List<DiaryEntry> getAllForUser(int userId);

    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<DiaryEntry>> getEntriesLiveData(int userId);

    @Query("SELECT * FROM diary_entries WHERE id = :entryId")
    DiaryEntry getById(int entryId);

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date = :date LIMIT 1")
    DiaryEntry getEntryByDate(int userId, String date);

    @Query("DELETE FROM diary_entries WHERE userId = :userId")
    void deleteAllForUser(int userId);
}