package com.example.fitnesshelper.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProgram(Program program);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProgramWithReturn(Program program);

    @Delete
    void deleteProgram(Program program);

    @Query("SELECT * FROM programs WHERE userId = :userId")
    List<Program> getProgramsForUser(int userId);

    @Query("SELECT * FROM programs WHERE userId = :userId")
    LiveData<List<Program>> getProgramsLiveData(int userId);

    @Query("SELECT COUNT(*) FROM programs WHERE userId = :userId")
    int getProgramCountForUser(int userId);

    @Query("DELETE FROM programs WHERE userId = :userId")
    void deleteAllForUser(int userId);
}