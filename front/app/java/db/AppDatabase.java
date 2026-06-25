package com.example.fitnesshelper.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        User.class,
        TrainingDate.class,
        Program.class,
        TrainingSession.class,
        Exercise.class,
        ExerciseSet.class,
        TrainingDay.class,
        DiaryEntry.class,
        DiaryExercise.class,
        DiaryExerciseSet.class
}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract TrainingDateDao trainingDateDao();
    public abstract ProgramDao programDao();
    public abstract TrainingSessionDao trainingSessionDao();
    public abstract ExerciseDao exerciseDao();
    public abstract ExerciseSetDao exerciseSetDao();
    public abstract TrainingDayDao trainingDayDao();
    public abstract DiaryEntryDao diaryEntryDao();
    public abstract DiaryExerciseDao diaryExerciseDao();
    public abstract DiaryExerciseSetDao diaryExerciseSetDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "fitness_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}